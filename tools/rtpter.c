#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <time.h>


/* Big Endian
typedef struct __attribute__((__packed__)) rtp_header {
  char version: 2;
  char padding: 1;
  char extension: 1;
  char csrc_cc: 4;
  char marker: 1;
  char payload_type: 7;
  unsigned short sequence_n;
  unsigned int timestamp;
  unsigned int ssrc;
} rtp_header_t;
*/

typedef struct __attribute__((__packed__)) rtp_header {
  char csrc_cc: 4;
  char extension: 1;
  char padding: 1;
  char version: 2;
  char payload_type: 7;
  char marker: 1;
  unsigned short sequence_n;
  unsigned int timestamp;
  unsigned int ssrc;
} rtp_header_t;



#define SSRC 1
#define DYNAMIC_RTP_TYPE 96

#define MTU 512

#define RTP_HDR_SIZE ( sizeof(rtp_header_t) )


void init_rtp_header(rtp_header_t *header) {
  memset((void *)header, 0, sizeof(rtp_header_t));
  header->version = 2;
  header->payload_type = DYNAMIC_RTP_TYPE;
}

int to_rtp(FILE *input, FILE *output) {
  int n;
  int sequence_n = 1;
  unsigned char buffer[MTU];
  rtp_header_t rtp_hdr;
  int ts;

  init_rtp_header(&rtp_hdr);

  while (!feof(input)) {
    ts = (int)time(NULL);

    rtp_hdr.sequence_n = sequence_n;
    rtp_hdr.timestamp = ts;

    memset((void *)buffer, 0, MTU);
    memcpy((void *)&buffer[0], &rtp_hdr, RTP_HDR_SIZE);
    n = fread(&buffer[RTP_HDR_SIZE], 1, MTU - RTP_HDR_SIZE, input);
    if (n <= 0) {
      return 1;
    }
    fwrite((void *)buffer, 1, MTU, output);
    sequence_n++;
  }

  return 0;
}


int main() {  
  return to_rtp(stdin, stdout);
}

