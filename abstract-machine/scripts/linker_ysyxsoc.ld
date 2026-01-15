/* =====================================================
 * ysyxsoc Linker Script - 完整版
 * 支持：
 *   - 一级 Bootloader (FSBL)
 *   - 二级 Bootloader (SSBL)
 *   - Application (.text/.rodata/.data/.bss)
 *   - RT-Thread extra sections (.data.extra/.bss.extra)
 *   - copy.table / zero.table 自动初始化
 * ===================================================== */

MEMORY {
  mrom  : ORIGIN = 0x20000000, LENGTH = 4K
  sram  : ORIGIN = 0x0f000000, LENGTH = 8K
  flash : ORIGIN = 0x30000000, LENGTH = 256M
  psram : ORIGIN = 0x80000000, LENGTH = 4M
  sdram : ORIGIN = 0xa0000000, LENGTH = 128M
}

ENTRY(_bootloader)

SECTIONS {

  /* =====================================================
   * Bootloader in Flash
   * ===================================================== */
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

  /* =====================================================
   * Application in SDRAM
   * ===================================================== */
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

  /* =====================================================
   * RT-Thread extra data section
   * ===================================================== */
  .data.extra : {
    . = ALIGN(8);
    _data_extra_start = .;

    /* finsh shell */
    __fsymtab_start = .;
    KEEP(*(FSymTab))
    __fsymtab_end = .;
    . = ALIGN(8);

    __vsymtab_start = .;
    KEEP(*(VSymTab))
    __vsymtab_end = .;
    . = ALIGN(8);

    /* initial functions */
    __rt_init_start = .;
    KEEP(*(SORT(.rti_fn*)))
    __rt_init_end = .;
    . = ALIGN(8);

    /* unit test table */
    __rt_utest_tc_tab_start = .;
    KEEP(*(UtestTcTab))
    __rt_utest_tc_tab_end = .;
    . = ALIGN(8);

    /* application extra data */
    __am_apps_data_start = .;
    *(__am_apps.data*)
    *(__am_apps.sdata*)
    __am_apps_data_end = .;

    . = ALIGN(8);
    _data_extra_end = .;
  } > sdram AT> flash
  _data_extra_load_start = LOADADDR(.data.extra);

  /* =====================================================
   * Normal data section
   * ===================================================== */
  .data : {
    . = ALIGN(8);
    _data_start = .;
    *(.data*)
    *(.sdata*)
    . = ALIGN(8);
  } > sdram AT> flash
  _data_end = .;
  _data_load_start = LOADADDR(.data);

  /* =====================================================
   * RT-Thread extra BSS
   * ===================================================== */
  .bss.extra (NOLOAD) : {
    . = ALIGN(8);
    _bss_extra_start = .;
    *(.bss.extra*)
    _bss_extra_end = .;
    . = ALIGN(8);
  } > sdram

  /* =====================================================
   * Normal BSS
   * ===================================================== */
  .bss (NOLOAD) : {
    . = ALIGN(8);
    _bss_start = .;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
    . = ALIGN(8);
  } > sdram
  _bss_end = .;

  /* =====================================================
   * Align before copy / zero tables
   * ===================================================== */
  . = ALIGN(8);

  /* =====================================================
   * Copy Table (for data / extra initialization)
   * ===================================================== */
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

  /* =====================================================
   * Zero Table (for BSS initialization)
   * ===================================================== */
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

  /* =====================================================
   * Stack / Heap
   * ===================================================== */
  _stack_top = ORIGIN(sdram) + LENGTH(sdram);
  . = _stack_top + 0x8000;
  _stack_pointer = .;
  end = .;
  _end = .;

  _heap_start = ALIGN(0x1000);
  _heap_end   = ORIGIN(sdram) + LENGTH(sdram);

  _sram_stack_pointer = ORIGIN(sram) + LENGTH(sram);
}