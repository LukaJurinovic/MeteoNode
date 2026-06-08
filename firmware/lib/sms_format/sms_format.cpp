#include "sms_format.h"
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>

float parseOrNan(const char* s) {
    if (strcmp(s, "nan") == 0 || strcmp(s, "NAN") == 0) return NAN;
    return (float)atof(s);
}

void fmtF(float v, int decimals, char* buf, int bufLen) {
    if (isnan(v)) { snprintf(buf, bufLen, "nan"); return; }
    snprintf(buf, bufLen, "%.*f", decimals, (double)v);
}

bool parseSmsBody(const char* csv, SmsBody* out) {
    char buf[256];
    strncpy(buf, csv, sizeof(buf) - 1);
    buf[sizeof(buf) - 1] = '\0';

    out->confCount = 0;

    const char* tokens[16];
    int count = 0;
    char* p = buf;
    while (*p && count < 16) {
        tokens[count++] = p;
        char* comma = strchr(p, ',');
        if (!comma) break;
        *comma = '\0';
        p = comma + 1;
    }

    if (count < 6) return false;

    strncpy(out->serial, tokens[0], sizeof(out->serial) - 1);
    out->serial[sizeof(out->serial) - 1] = '\0';
    out->temp = parseOrNan(tokens[1]);
    out->pressure = parseOrNan(tokens[2]);
    out->humidity = parseOrNan(tokens[3]);
    out->uv = parseOrNan(tokens[4]);
    out->lux = parseOrNan(tokens[5]);

    for (int i = 6; i < count && out->confCount < SMS_CONF_MAX; i++) {
        const char* tok = tokens[i];
        while (*tok == ' ') tok++;
        if (strncmp(tok, "CONF:", 5) == 0)
            out->confIds[out->confCount++] = atoi(tok + 5);
    }
    return true;
}

bool parseCommand(const char* line, ParsedCommand* out) {
    char buf[128];
    strncpy(buf, line, sizeof(buf) - 1);
    buf[sizeof(buf) - 1] = '\0';

    const char* parts[4];
    int count = 0;
    char* p = buf;
    while (*p && count < 4) {
        parts[count++] = p;
        char* colon = strchr(p, ':');
        if (!colon) break;
        *colon = '\0';
        p = colon + 1;
    }

    if (count < 4) return false;

    // parts[0] = "CMD", parts[1] = type, parts[2] = payload, parts[3] = cmdId
    strncpy(out->type, parts[1], sizeof(out->type) - 1);
    out->type[sizeof(out->type) - 1] = '\0';
    strncpy(out->payload, parts[2], sizeof(out->payload) - 1);
    out->payload[sizeof(out->payload) - 1] = '\0';
    out->cmdId = atoi(parts[3]);
    return true;
}

int splitBatch(const char* batch, ParsedCommand* out, int max) {
    char buf[512];
    strncpy(buf, batch, sizeof(buf) - 1);
    buf[sizeof(buf) - 1] = '\0';

    int count = 0;
    char* p = buf;
    while (*p && count < max) {
        char* semi = strchr(p, ';');
        if (semi) *semi = '\0';
        if (parseCommand(p, &out[count])) count++;
        if (!semi) break;
        p = semi + 1;
    }
    return count;
}

int buildBatchSms(const ParsedCommand* cmds, int n, char* out, int outLen) {
    int written = 0;
    for (int i = 0; i < n; i++) {
        if (i > 0 && written < outLen - 1)
            written += snprintf(out + written, outLen - written, ";");
        if (written < outLen - 1)
            written += snprintf(out + written, outLen - written,
                                "CMD:%s:%s:%d",
                                cmds[i].type, cmds[i].payload, cmds[i].cmdId);
    }
    return written;
}

int buildNodeSms(const char* serial, float t, float p, float h,
                 float uv, float lux,
                 const int* ackIds, int ackCount,
                 char* out, int outLen) {
    char tf[16], pf[16], hf[16], uvf[16], luxf[16];
    fmtF(t, 2, tf, sizeof(tf));
    fmtF(p, 2, pf, sizeof(pf));
    fmtF(h, 1, hf, sizeof(hf));
    fmtF(uv, 2, uvf, sizeof(uvf));
    fmtF(lux, 1, luxf, sizeof(luxf));

    int written = snprintf(out, outLen, "%s,%s,%s,%s,%s,%s",
                           serial, tf, pf, hf, uvf, luxf);

    for (int i = 0; i < ackCount; i++) {
        if (written >= outLen - 1) break;
        written += snprintf(out + written, outLen - written, ",CONF:%d", ackIds[i]);
    }
    return written;
}

bool parseCapsBody(const char* line, CapsBody* out) {
    if (strncmp(line, "CAPS:", 5) != 0) return false;
    char buf[128];
    strncpy(buf, line + 5, sizeof(buf) - 1);
    buf[sizeof(buf) - 1] = '\0';

    char* colon = strchr(buf, ':');
    if (!colon) return false;
    *colon = '\0';

    strncpy(out->serial, buf, sizeof(out->serial) - 1);
    out->serial[sizeof(out->serial) - 1] = '\0';

    out->sensorCount = 0;
    char* p = colon + 1;
    while (*p && out->sensorCount < SMS_SENSOR_LIST_MAX) {
        char* comma = strchr(p, ',');
        if (comma) *comma = '\0';
        strncpy(out->sensors[out->sensorCount], p, SMS_SENSOR_NAME_MAX - 1);
        out->sensors[out->sensorCount][SMS_SENSOR_NAME_MAX - 1] = '\0';
        out->sensorCount++;
        if (!comma) break;
        p = comma + 1;
    }
    return out->sensorCount > 0;
}

int buildCapsSms(const char* serial, const char* const* sensors, int count, char* out, int outLen) {
    int n = snprintf(out, outLen, "CAPS:%s:", serial);
    int written = (n >= outLen) ? outLen - 1 : n;
    for (int i = 0; i < count; i++) {
        if (i > 0 && written < outLen - 1) {
            n = snprintf(out + written, outLen - written, ",");
            written += (n >= outLen - written) ? outLen - written - 1 : n;
        }
        if (written < outLen - 1) {
            n = snprintf(out + written, outLen - written, "%s", sensors[i]);
            written += (n >= outLen - written) ? outLen - written - 1 : n;
        }
    }
    return written;
}
