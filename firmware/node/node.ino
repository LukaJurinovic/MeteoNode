#include <Wire.h>
#include <DHT.h>
#include <BH1750.h>
#include <HardwareSerial.h>
#include <esp_task_wdt.h>
#include <Preferences.h>
#include <sms_format.h>
#include "config.h"

#define BMP280_REG_CHIPID 0xD0
#define BMP280_REG_CALIB 0x88
#define BMP280_REG_CONTROL 0xF4
#define BMP280_REG_PRESSMSB 0xF7
static uint8_t _bmpAddr;
static uint16_t _bT1;
static int16_t _bT2, _bT3;
static uint16_t _bP1;
static int16_t _bP2, _bP3, _bP4, _bP5, _bP6, _bP7, _bP8, _bP9;
static int32_t _bmpFine;

static uint8_t bmpRead8(uint8_t reg) {
  Wire.beginTransmission(_bmpAddr); Wire.write(reg); Wire.endTransmission(false);
  Wire.requestFrom(_bmpAddr, (uint8_t)1); return Wire.read();
}

static bool bmpBegin() {
  Wire.setClock(100000);
  Wire.setTimeOut(50);
  uint8_t addrs[] = {0x76, 0x77};
  for (int i = 0; i < 2; i++) {
    _bmpAddr = addrs[i];
    uint8_t id = bmpRead8(BMP280_REG_CHIPID);
    if (id != 0x58 && id != 0x60) continue; // 0x58=BMP280, 0x60=BME280
    Wire.beginTransmission(_bmpAddr); Wire.write(BMP280_REG_CALIB); Wire.endTransmission(false);
    Wire.requestFrom(_bmpAddr, (uint8_t)24);
    uint8_t b[24]; for (int j = 0; j < 24; j++) b[j] = Wire.read();
    _bT1 = (uint16_t)(b[1]<<8|b[0]); _bT2 = (int16_t)(b[3]<<8|b[2]); _bT3 = (int16_t)(b[5]<<8|b[4]);
    _bP1 = (uint16_t)(b[7]<<8|b[6]); _bP2 = (int16_t)(b[9]<<8|b[8]); _bP3 = (int16_t)(b[11]<<8|b[10]);
    _bP4 = (int16_t)(b[13]<<8|b[12]); _bP5 = (int16_t)(b[15]<<8|b[14]); _bP6 = (int16_t)(b[17]<<8|b[16]);
    _bP7 = (int16_t)(b[19]<<8|b[18]); _bP8 = (int16_t)(b[21]<<8|b[20]); _bP9 = (int16_t)(b[23]<<8|b[22]);
    Wire.beginTransmission(_bmpAddr); Wire.write(BMP280_REG_CONTROL); Wire.write(0x27); Wire.endTransmission();
    delay(100);
    return true;
  }
  return false;
}

static float bmpReadTemperature() {
  Wire.beginTransmission(_bmpAddr); Wire.write(0xFA); Wire.endTransmission(false);
  Wire.requestFrom(_bmpAddr, (uint8_t)3);
  uint8_t msb = Wire.read();
  uint8_t lsb = Wire.read();
  uint8_t xlsb = Wire.read();
  int32_t raw = ((int32_t)msb<<12)|((int32_t)lsb<<4)|(xlsb>>4);
  int32_t v1 = ((((raw>>3)-((int32_t)_bT1<<1)))*((int32_t)_bT2))>>11;
  int32_t v2 = (((((raw>>4)-(int32_t)_bT1)*((raw>>4)-(int32_t)_bT1))>>12)*((int32_t)_bT3))>>14;
  _bmpFine = v1+v2;
  return (float)((_bmpFine*5+128)>>8)/100.0f;
}

static float bmpReadPressure() {
  Wire.beginTransmission(_bmpAddr); Wire.write(BMP280_REG_PRESSMSB); Wire.endTransmission(false);
  Wire.requestFrom(_bmpAddr, (uint8_t)3);
  uint8_t msb = Wire.read();
  uint8_t lsb = Wire.read();
  uint8_t xlsb = Wire.read();
  int32_t raw = ((int32_t)msb<<12)|((int32_t)lsb<<4)|(xlsb>>4);
  int64_t v1 = (int64_t)_bmpFine-128000;
  int64_t v2 = v1*v1*(int64_t)_bP6; v2 += (v1*(int64_t)_bP5)<<17; v2 += ((int64_t)_bP4)<<35;
  v1 = ((v1*v1*(int64_t)_bP3)>>8)+((v1*(int64_t)_bP2)<<12);
  v1 = (((int64_t)1<<47)+v1)*(int64_t)_bP1>>33;
  if (v1==0) return NAN;
  int64_t p = 1048576-raw; p = (((p<<31)-v2)*3125)/v1;
  v1 = ((int64_t)_bP9*(p>>13)*(p>>13))>>25; v2 = ((int64_t)_bP8*p)>>19;
  return (float)(((p+v1+v2)>>8)+((int64_t)_bP7<<4))/256.0f/100.0f;
}

#define GSM_RX 16
#define GSM_TX 17
#define I2C_SDA 21
#define I2C_SCL 22
#define DHT_PIN 26
#define DHT_TYPE DHT11
#define GUVA_PIN 34

#define WDT_TIMEOUT_S 30
#define GSM_INIT_RETRIES 5
#define SMS_RETRIES 3

#define PENDING_ACK_MAX 4
#define CMD_BODY_TIMEOUT_MS 5000UL

#define CMD_DEDUP_MAX 8
#define CMD_DEDUP_WINDOW_MS 120000UL  // ignore a repeated cmdId seen within this window

HardwareSerial gsm(2);
DHT dht(DHT_PIN, DHT_TYPE);
BH1750 lightMeter;
Preferences prefs;

unsigned long reportIntervalSecs = 86400UL;
unsigned long lastSend = 0;
String smsBuffer = "";
bool expectsCmd = false;
unsigned long expectsSince = 0;
bool bmpOk = false;
bool lightOk = false;

int pendingAcks[PENDING_ACK_MAX] = {-1, -1, -1, -1};
int pendingAckCount = 0;

int dedupIds[CMD_DEDUP_MAX] = {-1, -1, -1, -1, -1, -1, -1, -1};
unsigned long dedupAt[CMD_DEDUP_MAX] = {0};
int dedupHead = 0;

void setup() {
  Serial.begin(115200);
  gsm.begin(9600, SERIAL_8N1, GSM_RX, GSM_TX);
  Wire.begin(I2C_SDA, I2C_SCL);
  smsBuffer.reserve(544);

#if ESP_IDF_VERSION >= ESP_IDF_VERSION_VAL(5, 0, 0)
  esp_task_wdt_config_t wdt = {
    .timeout_ms = WDT_TIMEOUT_S * 1000,
    .idle_core_mask = 0,
    .trigger_panic = true
  };
  esp_task_wdt_init(&wdt);
#else
  esp_task_wdt_init(WDT_TIMEOUT_S, true);
#endif
  esp_task_wdt_add(NULL);

  prefs.begin("node", false);
  if (prefs.getString("deployId", "") != String(DEPLOY_ID)) {
    Serial.println("New deployment, clearing node state.");
    prefs.clear();
    prefs.putString("deployId", DEPLOY_ID);
  }
  reportIntervalSecs = prefs.getULong("interval", 86400UL);
  if (reportIntervalSecs > 86400UL) {
    reportIntervalSecs /= 1000UL;
    prefs.putULong("interval", reportIntervalSecs);
  }

  if (reportIntervalSecs < 10UL || reportIntervalSecs > 86400UL) {
    reportIntervalSecs = 86400UL;
    prefs.putULong("interval", reportIntervalSecs);
  }

  bmpOk = bmpBegin();
  if (!bmpOk) Serial.println("BMP280 missing.");

  lightOk = lightMeter.begin(BH1750::CONTINUOUS_HIGH_RES_MODE);
  if (!lightOk) Serial.println("GY-302 missing.");

  dht.begin();
  delay(2000);

  initGSM();

  if (!prefs.getBool("capsSent", false) || !prefs.isKey("serial") || prefs.getString("serial", "") != String(SERIAL_NUMBER)) {
    const char* sensors[4];
    int count = 0;
    if (bmpOk) sensors[count++] = "BMP280";
    sensors[count++] = "DHT11";
    sensors[count++] = "GUVA_S12SD";
    if (lightOk) sensors[count++] = "GY_302";
    char capsMsg[64];
    buildCapsSms(SERIAL_NUMBER, sensors, count, capsMsg, sizeof(capsMsg));
    if (sendSMS(GATEWAY_NUMBER, String(capsMsg))) {
      prefs.putBool("capsSent", true);
      prefs.putString("serial", SERIAL_NUMBER);
    } else {
      Serial.println("CAPS register SMS failed; will retry on next boot.");
    }
  }

  sendReadings();
  lastSend = millis();

  Serial.println("Node ready. Interval=" + String(reportIntervalSecs) + "s");
}

void loop() {
  esp_task_wdt_reset();

  int rxBudget = 1024;
  while (gsm.available() && rxBudget-- > 0) {
    esp_task_wdt_reset();
    char c = gsm.read();
    smsBuffer += c;
    if (smsBuffer.length() > 512) { smsBuffer = ""; Serial.println("smsBuffer overflow, reset."); }
    if (smsBuffer.endsWith("\n")) {
      String line = smsBuffer;
      smsBuffer = "";
      line.trim();

      if (line.startsWith("+CMT:")) {
        expectsCmd = true;
        expectsSince = millis();
      } else if (expectsCmd) {
        if (millis() - expectsSince > CMD_BODY_TIMEOUT_MS) {
          expectsCmd = false;
          Serial.println("CMD body timeout, discarding.");
        } else if (line.length() > 0) {
          expectsCmd = false;
          if (line.startsWith("CMD:")) handleCommand(line);
        }
      }
    }
  }

  if (millis() - lastSend >= reportIntervalSecs * 1000UL) {
    lastSend = millis();
    sendReadings();
  }
}

void initGSM() {
  delay(3000);
  gsm.write(27);
  delay(200);
  while (gsm.available()) gsm.read();
  for (int attempt = 1; attempt <= GSM_INIT_RETRIES; attempt++) {
    esp_task_wdt_reset();
    Serial.println("GSM init attempt " + String(attempt));
    gsmCmd("AT");
    {
      String pinStatus = "";
      for (int w = 0; w < 15; w++) {
        esp_task_wdt_reset();
        pinStatus = gsmCmd("AT+CPIN?");
        if (pinStatus.indexOf("ERROR") < 0) break;
        delay(1000);
      }
      if (pinStatus.indexOf("SIM PIN") >= 0) {
        const char* pins[] = SIM_PINS;
        for (int p = 0; p < (int)(sizeof(pins) / sizeof(pins[0])); p++) {
          String r = gsmCmd(String("AT+CPIN=") + pins[p]);
          if (r.indexOf("OK") >= 0) break;
          delay(3000);
        }
      }
    }
    gsmCmd("AT+COPS=0");

    bool registered = false;
    for (int i = 0; i < 30; i++) {
      String reg = gsmCmd("AT+CREG?");
      if (reg.indexOf(",1") >= 0 || reg.indexOf(",5") >= 0) { registered = true; break; }
      esp_task_wdt_reset();
      delay(1000);
    }

    if (!registered) { Serial.println("No signal."); continue; }

    gsmCmd("AT+COPS?");
    {
      String r = gsmCmd("AT+CSCS=\"IRA\"");
      if (r.indexOf("OK") < 0) Serial.println("CSCS IRA failed: " + r);
    }
    gsmCmd("AT+CMGF=1");
    gsmCmd("AT+CNMI=2,2,0,0,0");
    gsmCmd("AT+CMGD=1,4");
    Serial.println("GSM ready.");
    return;
  }
  Serial.println("GSM init failed, rebooting.");
  ESP.restart();
}

void sendReadings() {
  float temp = bmpOk ? bmpReadTemperature() : NAN;
  float pres = bmpOk ? bmpReadPressure() : NAN;

  float hum = dht.readHumidity();
  if (isnan(hum)) { delay(2000); hum = dht.readHumidity(); }
  float rawUv = (analogRead(GUVA_PIN) * 3.3 / 4095.0) * 10.0;
  float uv = (rawUv > 20.0f) ? NAN : rawUv;
  float rawLux = lightOk ? lightMeter.readLightLevel() : NAN;
  float lux = (rawLux < 0) ? NAN : rawLux;

  char msg[128];
  buildNodeSms(SERIAL_NUMBER, temp, pres, hum, uv, lux,
               pendingAcks, pendingAckCount, msg, sizeof(msg));
  pendingAckCount = 0;
  sendSMS(GATEWAY_NUMBER, String(msg));
}

void processCommand(const ParsedCommand& cmd) {
  if (strcmp(cmd.type, "REBOOT") == 0) {
    sendSMS(GATEWAY_NUMBER, "CONF:" + String(cmd.cmdId));
    delay(500);
    ESP.restart();
  } else if (strcmp(cmd.type, "REQUEST_READINGS") == 0) {
    if (pendingAckCount < PENDING_ACK_MAX)
      pendingAcks[pendingAckCount++] = cmd.cmdId;
  } else if (strcmp(cmd.type, "SET_INTERVAL") == 0) {
    long rawSecs = atol(cmd.payload);
    if (rawSecs >= 10 && rawSecs <= 86400) {
      reportIntervalSecs = (unsigned long)rawSecs;
      prefs.putULong("interval", reportIntervalSecs);
      Serial.println("Interval set to " + String(rawSecs) + "s");
    } else {
      Serial.println("SET_INTERVAL out of range (" + String(rawSecs) + "), ignoring.");
    }
    if (pendingAckCount < PENDING_ACK_MAX)
      pendingAcks[pendingAckCount++] = cmd.cmdId;
  }
}

bool cmdRecentlyProcessed(int cmdId) {
  unsigned long now = millis();
  for (int i = 0; i < CMD_DEDUP_MAX; i++)
    if (dedupIds[i] == cmdId && now - dedupAt[i] < CMD_DEDUP_WINDOW_MS) return true;
  return false;
}

void rememberCmd(int cmdId) {
  dedupIds[dedupHead] = cmdId;
  dedupAt[dedupHead] = millis();
  dedupHead = (dedupHead + 1) % CMD_DEDUP_MAX;
}

void handleCommand(const String& line) {
  ParsedCommand batch[PENDING_ACK_MAX];
  int n = splitBatch(line.c_str(), batch, PENDING_ACK_MAX);
  bool needReadings = false;
  for (int i = 0; i < n; i++) {
    if (cmdRecentlyProcessed(batch[i].cmdId)) {
      Serial.println("Duplicate cmdId " + String(batch[i].cmdId) + ", skipping.");
      continue;
    }
    rememberCmd(batch[i].cmdId);
    if (strcmp(batch[i].type, "REQUEST_READINGS") == 0) needReadings = true;
    processCommand(batch[i]);
  }

  if (needReadings) sendReadings();
}

bool sendSMS(const char* number, const String& text) {
  for (int attempt = 1; attempt <= SMS_RETRIES; attempt++) {
    esp_task_wdt_reset();
    gsm.print("AT+CMGS=\""); gsm.print(number); gsm.println("\"");
    delay(1000);
    gsm.print(text);
    gsm.write(26);

    long start = millis();
    String response = "";
    while (millis() - start < 8000) {
      esp_task_wdt_reset();
      while (gsm.available()) response += (char)gsm.read();
      if (response.indexOf("+CMGS:") >= 0) { Serial.println("SMS sent: " + text); return true; }
      if (response.indexOf("ERROR") >= 0) break;
      delay(100);
    }
    Serial.println("SMS attempt " + String(attempt) + " failed. Response: [" + response + "]");
    delay(2000 * attempt);
  }
  return false;
}

String gsmCmd(const String& cmd) {
  gsm.println(cmd);
  delay(600);
  String r = "";
  while (gsm.available()) r += (char)gsm.read();
  Serial.print("GSM: "); Serial.println(r);
  return r;
}