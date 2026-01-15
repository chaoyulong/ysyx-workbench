/* ================= ysyxsoc Linker Script ================= */

MEMORY {
  mrom  : ORIGIN = 0x20000000, LENGTH = 4K
  sram  : ORIGIN = 0x0f000000, LENGTH = 8K
  flash : ORIGIN = 0x30000000, LENGTH = 256M
  psram : ORIGIN = 0x80000000, LENGTH = 4M
  sdram : ORIGIN = 0xa0000000, LENGTH = 128M
}

ENTRY(_bootloader)

SECTIONS {

  /* ================= Boot in flash ================= */

  . = ORIGIN(flash);

  .bootloader : {
    . = ALIGN(8);
    *(load)
    . = ALIGN(8);
  } > flash

  .fsbl : {
    . = ALIGN(8);
    *(fsbl)
    . = ALIGN(8);
  } > flash

  .ssbl : {
    . = ALIGN(8);
    _ssbl_start = .;
    *(ssbl)
    . = ALIGN(8);
  } > sram AT> flash
  _ssbl_end = .;
  _ssbl_load_start = LOADADDR(.ssbl);

  /* ================= Application in SDRAM ================= */

  .text : {
    . = ALIGN(8);
    _text_start = .;
    *(entry)
    *(.text*)
    . = ALIGN(8);
  } > sdram AT> flash
  _text_end = .;
  _text_load_start = LOADADDR(.text);

  .rodata : {
    . = ALIGN(8);
    _rodata_start = .;
    *(.rodata*)
    *(.srodata*)
    . = ALIGN(8);
  } > sdram AT> flash
  _rodata_end = .;
  _rodata_load_start = LOADADDR(.rodata);

  /* ================= RT-Thread extra data ================= */

  .data.extra : {
    . = ALIGN(8);
    _data_extra_start = .;
    *(.data.extra*)
    _data_extra_end = .;
    . = ALIGN(8);
  } > sdram AT> flash
  _data_extra_load_start = LOADADDR(.data.extra);

  /* ================= Normal data ================= */

  .data : {
    . = ALIGN(8);
    _data_start = .;
    *(.data*)
    *(.sdata*)
    . = ALIGN(8);
  } > sdram AT> flash
  _data_end = .;
  _data_load_start = LOADADDR(.data);

  /* ================= RT-Thread extra bss ================= */

  .bss.extra (NOLOAD) : {
    . = ALIGN(8);
    _bss_extra_start = .;
    *(.bss.extra*)
    _bss_extra_end = .;
    . = ALIGN(8);
  } > sdram

  /* ================= Normal bss ================= */

  .bss (NOLOAD) : {
    . = ALIGN(8);
    _bss_start = .;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
    . = ALIGN(8);
  } > sdram
  _bss_end = .;

  /* ================= Copy Table ================= */

  .copy.table : {
    __copy_table_start = .;

    /* text */
    LONG(_text_load_start);
    LONG(_text_start);
    LONG(_text_end - _text_start);

    /* rodata */
    LONG(_rodata_load_start);
    LONG(_rodata_start);
    LONG(_rodata_end - _rodata_start);

    /* data.extra */
    LONG(_data_extra_load_start);
    LONG(_data_extra_start);
    LONG(_data_extra_end - _data_extra_start);

    /* data */
    LONG(_data_load_start);
    LONG(_data_start);
    LONG(_data_end - _data_start);

    __copy_table_end = .;
  } > flash

  /* ================= Zero Table ================= */

  .zero.table : {
    __zero_table_start = .;

    /* bss.extra */
    LONG(_bss_extra_start);
    LONG(_bss_extra_end - _bss_extra_start);

    /* bss */
    LONG(_bss_start);
    LONG(_bss_end - _bss_start);

    __zero_table_end = .;
  } > flash

  /* ================= Stack / Heap ================= */

  _stack_top = ORIGIN(sdram) + LENGTH(sdram);
  . = _stack_top + 0x8000;
  _stack_pointer = .;
  end = .;
  _end = .;

  _heap_start = ALIGN(0x1000);
  _heap_end   = ORIGIN(sdram) + LENGTH(sdram);

  _sram_stack_pointer = ORIGIN(sram) + LENGTH(sram);
}