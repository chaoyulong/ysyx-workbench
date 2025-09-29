/***************************************************************************************
* Copyright (c) 2014-2024 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include <common.h>
#include <device/map.h>
#include <SDL2/SDL.h>

enum {
  reg_freq,
  reg_channels,
  reg_samples,
  reg_sbuf_size,
  reg_init,
  reg_count,
  nr_reg
};

static uint8_t *sbuf = NULL;
static uint32_t *audio_base = NULL;
static uint32_t pAudio_pos = 0; // 开头位置

void fill_audio_buffer(void *userdata, uint8_t *stream, int len)
{
  uint32_t sbuf_size = audio_base[reg_sbuf_size];
	SDL_memset(stream, 0, len);

	len = (len > audio_base[reg_count] ? audio_base[reg_count] : len);
  if(pAudio_pos + len > sbuf_size)
  {
    uint32_t offset = sbuf_size - pAudio_pos; // 从pos到数组末尾的长度
    SDL_MixAudio(stream, sbuf + pAudio_pos, offset, SDL_MIX_MAXVOLUME);
    SDL_MixAudio(stream + offset, sbuf, len - offset, SDL_MIX_MAXVOLUME);
    pAudio_pos = len - offset;
  }
  else
  {
	  SDL_MixAudio(stream, sbuf + pAudio_pos, len, SDL_MIX_MAXVOLUME);
    pAudio_pos = pAudio_pos + len;
  }
	audio_base[reg_count] -= len;
}

void init_audio_config()
{
  SDL_AudioSpec s = {};
  
  s.freq = audio_base[reg_freq];
  s.format = AUDIO_S16SYS;  // 假设系统中音频数据的格式总是使用16位有符号数来表示
  s.channels = audio_base[reg_channels];
  s.silence = 0;
  s.samples = audio_base[reg_samples];
  s.size = CONFIG_SB_SIZE;
  s.callback = fill_audio_buffer;
  s.userdata = NULL;        // 不使用

  SDL_InitSubSystem(SDL_INIT_AUDIO);
  SDL_OpenAudio(&s, NULL);
  SDL_PauseAudio(0);
}

static void audio_io_handler(uint32_t offset, int len, bool is_write) {
  if(audio_base[reg_init] == 1)
  {
    init_audio_config();
    audio_base[reg_init] = 0;
  }
}

void init_audio() {
  uint32_t space_size = sizeof(uint32_t) * nr_reg;
  audio_base = (uint32_t *)new_space(space_size);
  audio_base[reg_sbuf_size] = CONFIG_SB_SIZE;
#ifdef CONFIG_HAS_PORT_IO
  add_pio_map ("audio", CONFIG_AUDIO_CTL_PORT, audio_base, space_size, audio_io_handler);
#else
  add_mmio_map("audio", CONFIG_AUDIO_CTL_MMIO, audio_base, space_size, audio_io_handler);
#endif

  sbuf = (uint8_t *)new_space(CONFIG_SB_SIZE);
  add_mmio_map("audio-sbuf", CONFIG_SB_ADDR, sbuf, CONFIG_SB_SIZE, NULL);
  memset(sbuf, 0, CONFIG_SB_SIZE);
}
