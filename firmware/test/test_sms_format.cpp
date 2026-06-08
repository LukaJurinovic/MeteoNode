#include <unity.h>
#include <sms_format.h>
#include <math.h>
#include <string.h>

void setUp(void) {}
void tearDown(void) {}

void test_parseOrNan_nan_lowercase(void) {
    TEST_ASSERT_TRUE(isnan(parseOrNan("nan")));
}

void test_parseOrNan_nan_uppercase(void) {
    TEST_ASSERT_TRUE(isnan(parseOrNan("NAN")));
}

void test_parseOrNan_positive_float(void) {
    TEST_ASSERT_FLOAT_WITHIN(0.001f, 1.5f, parseOrNan("1.5"));
}

void test_parseOrNan_zero(void) {
    TEST_ASSERT_FLOAT_WITHIN(0.001f, 0.0f, parseOrNan("0"));
}

void test_parseOrNan_negative(void) {
    TEST_ASSERT_FLOAT_WITHIN(0.001f, -3.14f, parseOrNan("-3.14"));
}

void test_fmtF_nan_writes_nan(void) {
    char buf[16];
    fmtF(NAN, 2, buf, sizeof(buf));
    TEST_ASSERT_EQUAL_STRING("nan", buf);
}

void test_fmtF_two_decimals(void) {
    char buf[16];
    fmtF(1.5f, 2, buf, sizeof(buf));
    TEST_ASSERT_EQUAL_STRING("1.50", buf);
}

void test_fmtF_one_decimal(void) {
    char buf[16];
    fmtF(22.34f, 1, buf, sizeof(buf));
    TEST_ASSERT_EQUAL_STRING("22.3", buf);
}

void test_parseSmsBody_valid_minimal(void) {
    SmsBody s;
    bool ok = parseSmsBody("NODE1,22.50,1013.25,55.0,1.20,300.5", &s);
    TEST_ASSERT_TRUE(ok);
    TEST_ASSERT_EQUAL_STRING("NODE1", s.serial);
    TEST_ASSERT_FLOAT_WITHIN(0.01f, 22.50f,   s.temp);
    TEST_ASSERT_FLOAT_WITHIN(0.01f, 1013.25f, s.pressure);
    TEST_ASSERT_FLOAT_WITHIN(0.01f, 55.0f,    s.humidity);
    TEST_ASSERT_FLOAT_WITHIN(0.01f, 1.20f,    s.uv);
    TEST_ASSERT_FLOAT_WITHIN(0.01f, 300.5f,   s.lux);
    TEST_ASSERT_EQUAL_INT(0, s.confCount);
}

void test_parseSmsBody_with_conf_tokens(void) {
    SmsBody s;
    bool ok = parseSmsBody("S1,10.0,1000.0,60.0,0.5,100.0,CONF:7,CONF:8", &s);
    TEST_ASSERT_TRUE(ok);
    TEST_ASSERT_EQUAL_INT(2, s.confCount);
    TEST_ASSERT_EQUAL_INT(7, s.confIds[0]);
    TEST_ASSERT_EQUAL_INT(8, s.confIds[1]);
}

void test_parseSmsBody_too_few_fields(void) {
    SmsBody s;
    bool ok = parseSmsBody("S1,10.0,1000.0,60.0", &s);
    TEST_ASSERT_FALSE(ok);
}

void test_parseSmsBody_nan_values(void) {
    SmsBody s;
    bool ok = parseSmsBody("NODE,nan,nan,nan,nan,nan", &s);
    TEST_ASSERT_TRUE(ok);
    TEST_ASSERT_TRUE(isnan(s.temp));
    TEST_ASSERT_TRUE(isnan(s.pressure));
}

void test_parseCommand_valid_no_payload(void) {
    ParsedCommand c;
    bool ok = parseCommand("CMD:REQUEST_READINGS::42", &c);
    TEST_ASSERT_TRUE(ok);
    TEST_ASSERT_EQUAL_STRING("REQUEST_READINGS", c.type);
    TEST_ASSERT_EQUAL_STRING("", c.payload);
    TEST_ASSERT_EQUAL_INT(42, c.cmdId);
}

void test_parseCommand_with_payload(void) {
    ParsedCommand c;
    bool ok = parseCommand("CMD:SET_INTERVAL:3600:5", &c);
    TEST_ASSERT_TRUE(ok);
    TEST_ASSERT_EQUAL_STRING("SET_INTERVAL", c.type);
    TEST_ASSERT_EQUAL_STRING("3600", c.payload);
    TEST_ASSERT_EQUAL_INT(5, c.cmdId);
}

void test_parseCommand_too_few_parts(void) {
    ParsedCommand c;
    bool ok = parseCommand("CMD:REQUEST_READINGS:only3", &c);
    TEST_ASSERT_FALSE(ok);
}

void test_splitBatch_single(void) {
    ParsedCommand batch[4];
    int n = splitBatch("CMD:REQUEST_READINGS::1", batch, 4);
    TEST_ASSERT_EQUAL_INT(1, n);
    TEST_ASSERT_EQUAL_STRING("REQUEST_READINGS", batch[0].type);
    TEST_ASSERT_EQUAL_INT(1, batch[0].cmdId);
}

void test_splitBatch_two_commands(void) {
    ParsedCommand batch[4];
    int n = splitBatch("CMD:REQUEST_READINGS::1;CMD:SET_INTERVAL:3600:2", batch, 4);
    TEST_ASSERT_EQUAL_INT(2, n);
    TEST_ASSERT_EQUAL_STRING("REQUEST_READINGS", batch[0].type);
    TEST_ASSERT_EQUAL_STRING("SET_INTERVAL", batch[1].type);
    TEST_ASSERT_EQUAL_STRING("3600", batch[1].payload);
    TEST_ASSERT_EQUAL_INT(2, batch[1].cmdId);
}

void test_splitBatch_bad_segment_skipped(void) {
    ParsedCommand batch[4];
    int n = splitBatch("CMD:REQUEST_READINGS::1;GARBAGE;CMD:REBOOT::3", batch, 4);
    TEST_ASSERT_EQUAL_INT(2, n);
}

void test_buildBatchSms_single(void) {
    ParsedCommand cmds[1] = {{"REQUEST_READINGS", "", 7}};
    char buf[64];
    buildBatchSms(cmds, 1, buf, sizeof(buf));
    TEST_ASSERT_EQUAL_STRING("CMD:REQUEST_READINGS::7", buf);
}

void test_buildBatchSms_two_commands(void) {
    ParsedCommand cmds[2] = {{"REQUEST_READINGS", "", 1}, {"REBOOT", "", 2}};
    char buf[64];
    buildBatchSms(cmds, 2, buf, sizeof(buf));
    TEST_ASSERT_EQUAL_STRING("CMD:REQUEST_READINGS::1;CMD:REBOOT::2", buf);
}

void test_buildNodeSms_basic(void) {
    char buf[128];
    buildNodeSms("N1", 22.50f, 1013.25f, 55.0f, 1.20f, 300.5f, nullptr, 0, buf, sizeof(buf));
    TEST_ASSERT_EQUAL_STRING("N1,22.50,1013.25,55.0,1.20,300.5", buf);
}

void test_buildNodeSms_with_acks(void) {
    int acks[] = {3, 7};
    char buf[128];
    buildNodeSms("N2", 20.0f, 1000.0f, 60.0f, 0.5f, 100.0f, acks, 2, buf, sizeof(buf));
    TEST_ASSERT_EQUAL_STRING("N2,20.00,1000.00,60.0,0.50,100.0,CONF:3,CONF:7", buf);
}

void test_parseCapsBody_all_four_sensors(void) {
    CapsBody c;
    bool ok = parseCapsBody("CAPS:NODE-001:BMP280,DHT11,GUVA_S12SD,GY_302", &c);
    TEST_ASSERT_TRUE(ok);
    TEST_ASSERT_EQUAL_STRING("NODE-001", c.serial);
    TEST_ASSERT_EQUAL_INT(4, c.sensorCount);
    TEST_ASSERT_EQUAL_STRING("BMP280", c.sensors[0]);
    TEST_ASSERT_EQUAL_STRING("GY_302", c.sensors[3]);
}

void test_parseCapsBody_partial_sensors(void) {
    CapsBody c;
    bool ok = parseCapsBody("CAPS:N2:BMP280,DHT11", &c);
    TEST_ASSERT_TRUE(ok);
    TEST_ASSERT_EQUAL_INT(2, c.sensorCount);
    TEST_ASSERT_EQUAL_STRING("DHT11", c.sensors[1]);
}

void test_parseCapsBody_wrong_prefix(void) {
    CapsBody c;
    TEST_ASSERT_FALSE(parseCapsBody("CMD:N2:BMP280", &c));
}

void test_buildCapsSms_two_sensors(void) {
    const char* s[] = {"BMP280", "DHT11"};
    char buf[64];
    buildCapsSms("N1", s, 2, buf, sizeof(buf));
    TEST_ASSERT_EQUAL_STRING("CAPS:N1:BMP280,DHT11", buf);
}

void test_buildCapsSms_all_four(void) {
    const char* s[] = {"BMP280", "DHT11", "GUVA_S12SD", "GY_302"};
    char buf[64];
    buildCapsSms("NODE-001", s, 4, buf, sizeof(buf));
    TEST_ASSERT_EQUAL_STRING("CAPS:NODE-001:BMP280,DHT11,GUVA_S12SD,GY_302", buf);
}

int main(void) {
    UNITY_BEGIN();
    RUN_TEST(test_parseOrNan_nan_lowercase);
    RUN_TEST(test_parseOrNan_nan_uppercase);
    RUN_TEST(test_parseOrNan_positive_float);
    RUN_TEST(test_parseOrNan_zero);
    RUN_TEST(test_parseOrNan_negative);
    RUN_TEST(test_fmtF_nan_writes_nan);
    RUN_TEST(test_fmtF_two_decimals);
    RUN_TEST(test_fmtF_one_decimal);
    RUN_TEST(test_parseSmsBody_valid_minimal);
    RUN_TEST(test_parseSmsBody_with_conf_tokens);
    RUN_TEST(test_parseSmsBody_too_few_fields);
    RUN_TEST(test_parseSmsBody_nan_values);
    RUN_TEST(test_parseCommand_valid_no_payload);
    RUN_TEST(test_parseCommand_with_payload);
    RUN_TEST(test_parseCommand_too_few_parts);
    RUN_TEST(test_splitBatch_single);
    RUN_TEST(test_splitBatch_two_commands);
    RUN_TEST(test_splitBatch_bad_segment_skipped);
    RUN_TEST(test_buildBatchSms_single);
    RUN_TEST(test_buildBatchSms_two_commands);
    RUN_TEST(test_buildNodeSms_basic);
    RUN_TEST(test_buildNodeSms_with_acks);
    RUN_TEST(test_parseCapsBody_all_four_sensors);
    RUN_TEST(test_parseCapsBody_partial_sensors);
    RUN_TEST(test_parseCapsBody_wrong_prefix);
    RUN_TEST(test_buildCapsSms_two_sensors);
    RUN_TEST(test_buildCapsSms_all_four);
    return UNITY_END();
}
