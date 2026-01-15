.section load, "ax"
.globl _bootloader
.type _bootloader, @function

_bootloader:
  mv s0, zero
  la sp, _sram_stack_pointer
  call _first_stage_bootloader
  fence.i
  call _second_stage_bootloader 
  fence.i 
  call _start


