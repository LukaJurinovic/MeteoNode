#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <HardwareSerial.h>
#include <esp_task_wdt.h>
#include <LittleFS.h>
#define SPIFFS LittleFS
#include <Preferences.h>
#include <time.h>
#include <sms_format.h>
#include "config.demo.h"

String ssid = WIFI_SSID;
String password = WIFI_PASSWORD;
String apiKey = API_KEY;
String serverUrl = SERVER_URL;

const char* ntpServer = "pool.ntp.org";

#define GSM_RX 16
#define GSM_TX 17

#define WDT_TIMEOUT_S 30
#define GSM_INIT_RETRIES 5
#define HTTP_RETRIES 3
#define SMS_RETRIES 3
#define MAX_NODES 5
#define COMMAND_CHECK_INTERVAL 30000UL
#define MAX_BATCH_COMMANDS 3
#define CMD_BODY_TIMEOUT_MS 5000UL  // reset expectingSmsBody if body never arrives

#define REGISTRY_JSON_CAPACITY 2048  // 5 nodes × ~350 bytes each
#define BATCH_JSON_CAPACITY 1024     // 5 measurements × ~200 bytes
#define REGISTER_JSON_CAPACITY 512
#define COMMAND_LIST_JSON_CAPACITY 1024

HardwareSerial gsm(2);

struct NodeRecord {
  String serialNumber;
  String phoneNumber;
  int nodeId = 0;
  int bmpId = 0;
  int dhtId = 0;
  int uvId = 0;
  int lightId = 0;
  int pendingReportCmdId = -1;
  bool active = false;
};
NodeRecord nodes[MAX_NODES];

unsigned long lastCommandCheck = 0;
String smsBuffer = "";
String pendingSender = "";
bool expectingSmsBody = false;
unsigned long expectingSince = 0;

void initWiFi();
int httpPost(const char* url, String& payload, String& responseBody);

void setup() {
  Serial.begin(115200);
  gsm.begin(9600, SERIAL_8N1, GSM_RX, GSM_TX);
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

  SPIFFS.begin(true);
  loadConfig();
  applyDeployReset();
  loadRegistry();

  initWiFi();
  syncNTP();
  initGSM();

  Serial.println("Gateway ready.");
  checkStoredSms();
}

void loop() {
  esp_task_wdt_reset();

  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi lost, reconnecting...");
    initWiFi();
  }

  int rxBudget = 1024;
  while (gsm.available() && rxBudget-- > 0) {
    esp_task_wdt_reset();
    char c = gsm.read();
    smsBuffer += c;
    if (smsBuffer.length() > 512) { smsBuffer = ""; Serial.println("smsBuffer overflow, reset."); }
    if (smsBuffer.endsWith("\n")) processSmsBuffer();
  }

  if (millis() - lastCommandCheck >= COMMAND_CHECK_INTERVAL) {
    lastCommandCheck = millis();
    checkPendingCommands();
    checkStoredSms();
  }
}

void loadConfig() {
  Preferences cfg;
  cfg.begin("gw", true);
  ssid = cfg.getString("ssid", ssid);
  password = cfg.getString("password", password);
  apiKey = cfg.getString("apiKey", apiKey);
  serverUrl = cfg.getString("serverUrl", serverUrl);
  cfg.end();
  Serial.println("Config: ssid=" + ssid + " server=" + serverUrl);
}

void initWiFi() {
  Serial.println("Connecting WiFi...");
  WiFi.begin(ssid.c_str(), password.c_str());
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED) {
    esp_task_wdt_reset();
    delay(500);
    if (++attempts > 40) { Serial.println("WiFi failed, rebooting."); ESP.restart(); }
  }
  Serial.println("WiFi connected.");
}

void syncNTP() {
  configTime(0, 0, ntpServer);
  struct tm t;
  int attempts = 0;
  while (!getLocalTime(&t)) {
    esp_task_wdt_reset();
    delay(500);
    if (++attempts > 20) { Serial.println("NTP timeout, continuing."); break; }
  }
  Serial.println("Time synced.");
}

void initGSM() {
  delay(3000);
  for (int attempt = 1; attempt <= GSM_INIT_RETRIES; attempt++) {
    esp_task_wdt_reset();
    Serial.println("GSM init attempt " + String(attempt));
    gsmCmd("AT");
    gsmCmd(String("AT+CPIN=") + SIM_PIN);
    gsmCmd("AT+COPS=0");

    bool registered = false;
    for (int i = 0; i < 30; i++) {
      String reg = gsmCmd("AT+CREG?");
      if (reg.indexOf(",1") >= 0 || reg.indexOf(",5") >= 0) { registered = true; break; }
      esp_task_wdt_reset();
      delay(1000);
    }

    gsmCmd("AT+CSQ");

    if (!registered) {
      Serial.println("No GSM signal. Scanning reachable operators...");
      gsmCmdLong("AT+COPS=?", 60000);
      continue;
    }

    gsmCmd("ATE0");
    gsmCmd("AT+COPS?");
    gsmCmd("AT+CSCS=\"IRA\"");
    gsmCmd("AT+CMGF=1");
    gsmCmd("AT+CSDH=0");
    gsmCmd("AT+CPMS=\"SM\",\"SM\",\"SM\"");
    gsmCmd("AT+CNMI=2,1,0,0,0");
    Serial.println("GSM ready.");
    return;
  }
  Serial.println("GSM init failed, rebooting.");
  ESP.restart();
}

void applyDeployReset() {
  Preferences cfg;
  cfg.begin("gw", false);
  String stored = cfg.getString("deployId", "");
  if (stored != DEPLOY_ID) {
    Serial.println("New deployment (\"" + stored + "\" -> \"" + String(DEPLOY_ID) +
                   "\"), wiping node registry.");
    SPIFFS.remove("/nodes.json");
    for (int i = 0; i < MAX_NODES; i++) nodes[i] = NodeRecord();
    cfg.putString("deployId", DEPLOY_ID);
  }
  cfg.end();
}

void loadRegistry() {
  if (!SPIFFS.exists("/nodes.json")) { Serial.println("No registry on flash."); return; }
  File f = SPIFFS.open("/nodes.json", "r");
  if (!f) { Serial.println("Failed to open registry."); return; }
  StaticJsonDocument<REGISTRY_JSON_CAPACITY> doc;
  if (deserializeJson(doc, f) != DeserializationError::Ok) {
    Serial.println("Registry parse error.");
    f.close();
    return;
  }
  f.close();
  int idx = 0;
  for (JsonObject o : doc.as<JsonArray>()) {
    if (idx >= MAX_NODES) break;
    nodes[idx].serialNumber = o["serialNumber"].as<String>();
    nodes[idx].phoneNumber = o["phoneNumber"].as<String>();
    nodes[idx].nodeId = o["nodeId"];
    nodes[idx].bmpId = o["bmpId"];
    nodes[idx].dhtId = o["dhtId"];
    nodes[idx].uvId = o["uvId"];
    nodes[idx].lightId = o["lightId"];
    nodes[idx].pendingReportCmdId = -1;
    nodes[idx].active = true;
    idx++;
  }
  Serial.println("Registry loaded: " + String(idx) + " node(s).");
}

void saveRegistry() {
  File f = SPIFFS.open("/nodes.json", "w");
  if (!f) { Serial.println("SPIFFS write failed."); return; }
  StaticJsonDocument<REGISTRY_JSON_CAPACITY> doc;
  JsonArray arr = doc.to<JsonArray>();
  for (int i = 0; i < MAX_NODES; i++) {
    if (!nodes[i].active) continue;
    JsonObject o = arr.createNestedObject();
    o["serialNumber"] = nodes[i].serialNumber;
    o["phoneNumber"] = nodes[i].phoneNumber;
    o["nodeId"] = nodes[i].nodeId;
    o["bmpId"] = nodes[i].bmpId;
    o["dhtId"] = nodes[i].dhtId;
    o["uvId"] = nodes[i].uvId;
    o["lightId"] = nodes[i].lightId;
  }
  if (serializeJson(doc, f) == 0) {
    Serial.println("registry write failed");
  }
  f.close();
}

void processSmsBuffer() {
  String line = smsBuffer;
  smsBuffer = "";
  line.trim();
  if (line.length() == 0) return;
  Serial.println("GSM rx: [" + line + "]");

  if (line.startsWith("+CMTI:")) {
    checkStoredSms();
    return;
  }

  if (line == "SMS Ready") {
    Serial.println("Modem reset detected, reinitializing GSM...");
    initGSM();
    return;
  }

  if (line.startsWith("+CMT:")) {
    int firstQuote = line.indexOf('"');
    if (firstQuote < 0) { Serial.println("+CMT parse error, skipping."); return; }
    int start = firstQuote + 1;
    int end = line.indexOf('"', start);
    if (end <= start) { Serial.println("+CMT parse error, skipping."); return; }
    pendingSender = line.substring(start, end);

    int lastQuote = line.lastIndexOf('"');
    if (lastQuote > end) {
      String inlineBody = line.substring(lastQuote + 1);
      inlineBody.trim();
      int bodyStart = 0;
      while (bodyStart < (int)inlineBody.length() && (unsigned char)inlineBody[bodyStart] < 0x20)
        bodyStart++;
      inlineBody = inlineBody.substring(bodyStart);
      if (inlineBody.length() > 0) {
        Serial.println("Inline SMS body: " + inlineBody);
        if (inlineBody.startsWith("CAPS:")) handleCapsSms(inlineBody, pendingSender);
        else if (inlineBody.startsWith("CONF:")) handleCommandConfirm(inlineBody, pendingSender);
        else handleNodeSMS(inlineBody, pendingSender);
        return;
      }
    }

    expectingSmsBody = true;
    expectingSince = millis();
    return;
  }

  if (expectingSmsBody) {
    if (millis() - expectingSince > CMD_BODY_TIMEOUT_MS) {
      expectingSmsBody = false;
      Serial.println("SMS body timeout, discarding.");
      return;
    }
    expectingSmsBody = false;
    if (line.startsWith("CAPS:")) {
      handleCapsSms(line, pendingSender);
    } else if (line.startsWith("CONF:")) {
      handleCommandConfirm(line, pendingSender);
    } else {
      handleNodeSMS(line, pendingSender);
    }
  }
}

void handleNodeSMS(const String& body, const String& senderNumber) {
  SmsBody sms;
  if (!parseSmsBody(body.c_str(), &sms)) {
    Serial.println("Bad SMS format, ignoring.");
    return;
  }

  String serial = String(sms.serial);
  NodeRecord* node = findNode(serial);
  if (!node) {
    Serial.println("Data SMS from unknown node '" + serial + "', ignoring (register via CAPS).");
    return;
  }

  char iso[25];
  if (!getIso(iso)) {
    Serial.println("Clock not NTP-synced; re-syncing and dropping batch from " + serial + ".");
    syncNTP();
    return;
  }

  StaticJsonDocument<BATCH_JSON_CAPACITY> doc;
  doc["nodeId"] = node->nodeId;
  JsonArray arr = doc.createNestedArray("measurements");
  if (node->bmpId > 0) addMeasurement(arr, node->bmpId, "TEMPERATURE", sms.temp, iso);
  if (node->bmpId > 0) addMeasurement(arr, node->bmpId, "PRESSURE", sms.pressure, iso);
  if (node->dhtId > 0) addMeasurement(arr, node->dhtId, "HUMIDITY", sms.humidity, iso);
  if (node->uvId > 0) addMeasurement(arr, node->uvId, "UV_INDEX", sms.uv, iso);
  if (node->lightId > 0) addMeasurement(arr, node->lightId, "ILLUMINANCE", sms.lux, iso);

  if (arr.size() == 0) {
    Serial.println("No valid readings for " + serial + " (all NaN), skipping POST.");
  } else {
    String payload, responseBody;
    serializeJson(doc, payload);
    String url = serverUrl + "/measurements";
    int code = httpPost(url.c_str(), payload, responseBody);
    Serial.println(code == 204
      ? "Measurements OK [" + serial + "]"
      : "Measurements failed: " + String(code) + " [" + serial + "]");

    if (code == 204 && node->pendingReportCmdId != -1) {
      confirmCommandById(node->serialNumber, node->pendingReportCmdId);
      node->pendingReportCmdId = -1;
    }
  }

  for (int i = 0; i < sms.confCount; i++)
    confirmCommandById(node->serialNumber, sms.confIds[i]);
}

NodeRecord* findNode(const String& serial) {
  for (int i = 0; i < MAX_NODES; i++)
    if (nodes[i].active && nodes[i].serialNumber == serial) return &nodes[i];
  return nullptr;
}

NodeRecord* findOrRegisterNode(const String& serial, const String& phoneNumber, const char** sensors, int sensorCount) {
  for (int i = 0; i < MAX_NODES; i++) {
    if (nodes[i].active && nodes[i].serialNumber == serial) {
      if (nodes[i].phoneNumber.isEmpty()) nodes[i].phoneNumber = phoneNumber;
      return &nodes[i];
    }
  }

  for (int i = 0; i < MAX_NODES; i++) {
    if (!nodes[i].active) {
      StaticJsonDocument<REGISTER_JSON_CAPACITY> doc;
      doc["serialNumber"] = serial;
      JsonArray s = doc.createNestedArray("sensors");
      for (int j = 0; j < sensorCount; j++) s.add(sensors[j]);

      String payload, responseBody;
      serializeJson(doc, payload);
      String url = serverUrl + "/nodes/register";
      int code = httpPost(url.c_str(), payload, responseBody);

      if (code == 200 || code == 201) {
        StaticJsonDocument<REGISTER_JSON_CAPACITY> res;
        DeserializationError err = deserializeJson(res, responseBody);
        if (err != DeserializationError::Ok || res["nodeId"].isNull()) {
          Serial.println("Node reg: bad JSON (" + String(err.c_str()) + ")");
          return nullptr;
        }
        nodes[i].serialNumber = serial;
        nodes[i].phoneNumber = phoneNumber;
        nodes[i].nodeId = res["nodeId"];
        nodes[i].bmpId = 0;
        nodes[i].dhtId = 0;
        nodes[i].uvId = 0;
        nodes[i].lightId = 0;
        for (int j = 0; j < sensorCount; j++) {
          if (res["sensorIds"][sensors[j]].isNull()) {
            Serial.println(String("Warning: missing sensorId for ") + sensors[j]);
            continue;
          }
          int id = res["sensorIds"][sensors[j]];
          if (strcmp(sensors[j], "BMP280") == 0) nodes[i].bmpId = id;
          else if (strcmp(sensors[j], "DHT11") == 0) nodes[i].dhtId = id;
          else if (strcmp(sensors[j], "GUVA_S12SD") == 0) nodes[i].uvId = id;
          else if (strcmp(sensors[j], "GY_302") == 0) nodes[i].lightId = id;
        }
        nodes[i].pendingReportCmdId = -1;
        nodes[i].active = true;
        saveRegistry();
        Serial.println("Node registered: " + serial + " nodeId=" + String(nodes[i].nodeId));
        return &nodes[i];
      }
      Serial.println("Node registration failed: " + String(code));
      return nullptr;
    }
  }

  Serial.println("Node registry full!");
  return nullptr;
}

void checkStoredSms() {
  Serial.println("Polling stored SMS...");
  gsmCmd("AT+CMGF=1");
  gsm.println("AT+CMGL=\"ALL\"");
  unsigned long start = millis();
  String resp = "";
  resp.reserve(512);
  while (millis() - start < 3000) {
    esp_task_wdt_reset();
    while (gsm.available()) resp += (char)gsm.read();
    if (resp.indexOf("\r\nOK") >= 0 || resp.indexOf("ERROR") >= 0) break;
    delay(50);
  }
  Serial.println("CMGL raw: [" + resp + "]");
  if (resp.indexOf("+CMT:") >= 0) {
    int pos = 0;
    while (true) {
      int cmtPos = resp.indexOf("+CMT:", pos);
      if (cmtPos < 0) break;
      int hdrEnd = resp.indexOf('\n', cmtPos);
      if (hdrEnd < 0) break;
      int q1 = resp.indexOf('"', cmtPos) + 1;
      int q2 = resp.indexOf('"', q1);
      String sender = (q2 > q1) ? resp.substring(q1, q2) : "";
      int bodyStart = hdrEnd + 1;
      int bodyEnd = resp.indexOf('\n', bodyStart);
      String body = resp.substring(bodyStart, bodyEnd >= 0 ? bodyEnd : resp.length());
      body.trim();
      int bs = 0;
      while (bs < (int)body.length() && (unsigned char)body[bs] < 0x20) bs++;
      body = body.substring(bs);
      if (body.length() > 0) {
        Serial.println("CMT during poll from " + sender + ": " + body);
        if (body.startsWith("CAPS:")) handleCapsSms(body, sender);
        else if (body.startsWith("CONF:")) handleCommandConfirm(body, sender);
        else handleNodeSMS(body, sender);
      }
      pos = (bodyEnd >= 0) ? bodyEnd + 1 : resp.length();
    }
  }
  if (resp.indexOf("+CMGL:") < 0) return;
  Serial.println("Stored SMS found, processing...");

  int pos = 0;
  while (true) {
    int hdr = resp.indexOf("+CMGL:", pos);
    if (hdr < 0) break;

    int commaAfterIdx = resp.indexOf(',', hdr + 6);
    int msgIdx = resp.substring(hdr + 6, commaAfterIdx).toInt();

    int q = hdr;
    for (int i = 0; i < 3; i++) { q = resp.indexOf('"', q + 1); if (q < 0) break; }
    if (q < 0) break;
    int senderStart = q + 1;
    int senderEnd = resp.indexOf('"', senderStart);
    String sender = resp.substring(senderStart, senderEnd);

    int lineEnd = resp.indexOf('\n', senderEnd);
    if (lineEnd < 0) break;
    int bodyStart = lineEnd + 1;
    int bodyEnd = resp.indexOf('\n', bodyStart);
    String body = (bodyEnd >= 0) ? resp.substring(bodyStart, bodyEnd) : resp.substring(bodyStart);
    body.trim();

    gsm.println("AT+CMGD=" + String(msgIdx));
    delay(400);
    esp_task_wdt_reset();

    if (body.length() > 0) {
      Serial.println("Stored SMS from " + sender + ": " + body);
      if (body.startsWith("CAPS:")) handleCapsSms(body, sender);
      else if (body.startsWith("CONF:")) handleCommandConfirm(body, sender);
      else handleNodeSMS(body, sender);
    }
    esp_task_wdt_reset();

    pos = (bodyEnd >= 0) ? bodyEnd : resp.length();
  }
}

void checkPendingCommands() {
  for (int i = 0; i < MAX_NODES; i++) {
    if (!nodes[i].active || nodes[i].phoneNumber.isEmpty()) continue;
    esp_task_wdt_reset();

    String responseBody;
    String url = serverUrl + "/nodes/" + nodes[i].serialNumber + "/commands/pending";
    int code = httpGet(url.c_str(), responseBody);

    if (code != 200 || responseBody.length() <= 2) continue;

    StaticJsonDocument<COMMAND_LIST_JSON_CAPACITY> doc;
    deserializeJson(doc, responseBody);

    ParsedCommand batch[MAX_BATCH_COMMANDS];
    int batchCount = 0;

    for (JsonObject cmd : doc.as<JsonArray>()) {
      if (batchCount >= MAX_BATCH_COMMANDS) break;
      int cmdId = cmd["id"];
      String command = cmd["command"].as<String>();
      String payload = cmd["payload"].isNull() ? "" : cmd["payload"].as<String>();

      strncpy(batch[batchCount].type, command.c_str(), sizeof(batch[batchCount].type) - 1);
      batch[batchCount].type[sizeof(batch[batchCount].type) - 1] = '\0';
      strncpy(batch[batchCount].payload, payload.c_str(), sizeof(batch[batchCount].payload) - 1);
      batch[batchCount].payload[sizeof(batch[batchCount].payload) - 1] = '\0';
      batch[batchCount].cmdId = cmdId;

      if (command == "REQUEST_READINGS") nodes[i].pendingReportCmdId = cmdId;

      batchCount++;
    }

    if (batchCount > 0) {
      char batchSmsStr[256];
      buildBatchSms(batch, batchCount, batchSmsStr, sizeof(batchSmsStr));
      bool sent = sendSMS(nodes[i].phoneNumber.c_str(), String(batchSmsStr));
      Serial.println(sent
        ? "Commands sent to " + nodes[i].serialNumber + ": " + String(batchSmsStr)
        : "Command SMS failed for " + nodes[i].serialNumber);
    }
  }
}

void confirmCommandById(const String& serialNumber, int cmdId) {
  String url = serverUrl + "/nodes/" + serialNumber
             + "/commands/" + String(cmdId) + "/confirm";
  String empty, responseBody;
  int code = httpPost(url.c_str(), empty, responseBody);
  Serial.println("Command " + String(cmdId) + " confirmed: HTTP " + String(code));
}

void handleCommandConfirm(const String& body, const String& senderNumber) {
  int cmdId = body.substring(5).toInt();
  for (int i = 0; i < MAX_NODES; i++) {
    if (nodes[i].active && nodes[i].phoneNumber == senderNumber) {
      confirmCommandById(nodes[i].serialNumber, cmdId);
      return;
    }
  }
  Serial.println("CONF from unknown sender: " + senderNumber);
}

void handleCapsSms(const String& body, const String& senderNumber) {
  CapsBody caps;
  if (!parseCapsBody(body.c_str(), &caps)) {
    Serial.println("Bad CAPS format, ignoring.");
    return;
  }
  const char* sensorPtrs[SMS_SENSOR_LIST_MAX];
  for (int i = 0; i < caps.sensorCount; i++) sensorPtrs[i] = caps.sensors[i];
  String serial = String(caps.serial);
  findOrRegisterNode(serial, senderNumber, sensorPtrs, caps.sensorCount);
  Serial.println("CAPS registered: " + serial + " (" + String(caps.sensorCount) + " sensors)");
}

int httpRequest(bool isPost, const char* url, String& payload, String& responseBody) {
  for (int attempt = 1; attempt <= HTTP_RETRIES; attempt++) {
    esp_task_wdt_reset();
    if (WiFi.status() != WL_CONNECTED) initWiFi();

    HTTPClient http;
    http.begin(url);
    http.addHeader("X-Api-Key", apiKey);
    http.setConnectTimeout(5000);  // bound TCP connect too — without this an
    http.setTimeout(5000);         // unreachable backend hangs ~30s and trips the WDT

    int code;
    if (isPost) {
      http.addHeader("Content-Type", "application/json");
      code = http.POST(payload);
    } else {
      code = http.GET();
    }
    esp_task_wdt_reset();
    if (code > 0) { responseBody = http.getString(); http.end(); return code; }
    http.end();
    Serial.println(String(isPost ? "POST" : "GET") + " attempt " + String(attempt) + " failed.");
    esp_task_wdt_reset();
    delay(1000 * attempt);
  }
  return -1;
}

int httpPost(const char* url, String& payload, String& responseBody) {
  return httpRequest(true, url, payload, responseBody);
}

int httpGet(const char* url, String& responseBody) {
  String empty;
  return httpRequest(false, url, empty, responseBody);
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
    Serial.println("SMS attempt " + String(attempt) + " failed.");
    delay(2000 * attempt);
  }
  return false;
}

void addMeasurement(JsonArray& arr, int sId, const char* metric, float val, const char* iso) {
  if (isnan(val)) return;
  JsonObject obj = arr.createNestedObject();
  obj["sensorId"] = sId;
  obj["metric"] = metric;
  obj["value"] = val;
  obj["measuredAt"] = iso;
}

bool getIso(char* buf) {
  time_t now; time(&now);
  if (now < 1700000000) return false;
  strftime(buf, 25, "%Y-%m-%dT%H:%M:%SZ", gmtime(&now));
  return true;
}

String gsmCmd(const String& cmd) {
  gsm.println(cmd);
  delay(600);
  String r = "";
  while (gsm.available()) r += (char)gsm.read();
  Serial.print("GSM: "); Serial.println(r);
  return r;
}

String gsmCmdLong(const String& cmd, unsigned long timeoutMs) {
  gsm.println(cmd);
  String r = "";
  unsigned long start = millis();
  while (millis() - start < timeoutMs) {
    esp_task_wdt_reset();
    while (gsm.available()) r += (char)gsm.read();
    if (r.indexOf("\r\nOK") >= 0 || r.indexOf("ERROR") >= 0) break;
    delay(50);
  }
  Serial.print("GSM: "); Serial.println(r);
  return r;
}