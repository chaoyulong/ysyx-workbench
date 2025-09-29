#include <am.h>
#include <nemu.h>
#include <string.h>

#define AUDIO_FREQ_ADDR      (AUDIO_ADDR + 0x00)
#define AUDIO_CHANNELS_ADDR  (AUDIO_ADDR + 0x04)
#define AUDIO_SAMPLES_ADDR   (AUDIO_ADDR + 0x08)
#define AUDIO_SBUF_SIZE_ADDR (AUDIO_ADDR + 0x0c)
#define AUDIO_INIT_ADDR      (AUDIO_ADDR + 0x10)
#define AUDIO_COUNT_ADDR     (AUDIO_ADDR + 0x14)

static uint8_t *fb = (uint8_t *)(uintptr_t)AUDIO_SBUF_ADDR;
static uint32_t pAudio_pos = 0; // 开头位置

void __am_audio_init() {
}

void __am_audio_config(AM_AUDIO_CONFIG_T *cfg) {
  cfg->present = true;
  cfg->bufsize = inl(AUDIO_SBUF_SIZE_ADDR);
}

void __am_audio_ctrl(AM_AUDIO_CTRL_T *ctrl) 
{
  outl(AUDIO_FREQ_ADDR, ctrl->freq);
  outl(AUDIO_CHANNELS_ADDR, ctrl->channels);
  outl(AUDIO_SAMPLES_ADDR, ctrl->samples);
  outl(AUDIO_INIT_ADDR, 1);
}

void __am_audio_status(AM_AUDIO_STATUS_T *stat) 
{
  stat->count = inl(AUDIO_COUNT_ADDR);
}

void __am_audio_play(AM_AUDIO_PLAY_T *ctl) 
{
  uint8_t *start = (ctl->buf).start;
  uint8_t *end = (ctl->buf).end;
  uint32_t len = end - start;
  int sbuf_size = inl(AUDIO_SBUF_SIZE_ADDR);
  int count = inl(AUDIO_COUNT_ADDR);

  while(sbuf_size - count < len) // 如果可用空间不足
  {
    count = inl(AUDIO_COUNT_ADDR);
  }

  if(pAudio_pos + len > sbuf_size)
  {
    uint32_t offset = sbuf_size - pAudio_pos; // 到数组最后的长度
    memcpy(fb + pAudio_pos, start, offset);
    memcpy(fb, start + offset, len - offset);
    pAudio_pos = len - offset;
  }
  else
  {
    memcpy(fb + pAudio_pos, start, len);
    pAudio_pos += len;
  }

  outl(AUDIO_COUNT_ADDR, count + len);
}
