#pragma once
#include <stddef.h>

#define SMS_SERIAL_MAX 32
#define SMS_CMD_TYPE_MAX 20
#define SMS_CMD_PAYLOAD_MAX 32
#define SMS_CONF_MAX 4
#define SMS_SENSOR_NAME_MAX 16
#define SMS_SENSOR_LIST_MAX 8

struct SmsBody {
    char  serial[SMS_SERIAL_MAX];
    float temp, pressure, humidity, uv, lux;
    int   confIds[SMS_CONF_MAX];
    int   confCount;
};

struct ParsedCommand {
    char type[SMS_CMD_TYPE_MAX];
    char payload[SMS_CMD_PAYLOAD_MAX];
    int  cmdId;
};

struct CapsBody {
    char serial[SMS_SERIAL_MAX];
    char sensors[SMS_SENSOR_LIST_MAX][SMS_SENSOR_NAME_MAX];
    int  sensorCount;
};

float parseOrNan(const char* s);
void fmtF(float v, int decimals, char* buf, int bufLen);
bool parseSmsBody(const char* csv, SmsBody* out);
bool parseCommand(const char* line, ParsedCommand* out);
int splitBatch(const char* batch, ParsedCommand* out, int max);
int buildBatchSms(const ParsedCommand* cmds, int n, char* out, int outLen);
int buildNodeSms(const char* serial, float t, float p, float h,
                 float uv, float lux,
                 const int* ackIds, int ackCount,
                 char* out, int outLen);
bool parseCapsBody(const char* line, CapsBody* out);
int buildCapsSms(const char* serial, const char* const* sensors, int count, char* out, int outLen);
