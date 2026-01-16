MEMORY {
  mrom  : ORIGIN = 0x20000000, LENGTH = 4K
  sram  : ORIGIN = 0x0f000000, LENGTH = 8K
  psram : ORIGIN = 0x80000000, LENGTH = 4M  
  flash : ORIGIN = 0x30000000, LENGTH = 256M
  sdram : ORIGIN = 0xa0000000, LENGTH = 128M 
}

ENTRY(_bootloader)

SECTIONS {
  /* _pmem_start and _entry_offset are defined in LDFLAGS */
  . = ORIGIN(flash);

  .bootloader : {
    . = ALIGN(8);
    *(load)
    . = ALIGN(8);
  } > flash AT> flash

  .fsbl : {
    . = ALIGN(8);
    *(fsbl)
    . = ALIGN(8);
  } > flash AT> flash

  .ssbl : {
    . = ALIGN(8);
    _ssbl_start = .;
    *(ssbl)
    . = ALIGN(8);
  } > sram AT> flash
  _ssbl_end = .;
  _ssbl_load_start = LOADADDR(.ssbl);

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
    *(.srodata.mainargs)  
    . = ALIGN(8);
  } > sdram AT> flash
  _rodata_end = .;
  _rodata_load_start = LOADADDR(.rodata);
 

.data : {
    . = ALIGN(8);
    _data_start = .;
    
    /* 在这里插入extra.ld的.data.extra段 */
    . = ALIGN(8);
    __data_extra_start = .;
    __fsymtab_start = .;
    KEEP(*(FSymTab))
    __fsymtab_end = .;
    . = ALIGN(8);
    __vsymtab_start = .;
    KEEP(*(VSymTab))
    __vsymtab_end = .;
    . = ALIGN(8);
    /* section information for initial. */
    . = ALIGN(8);
    __rt_init_start = .;
    KEEP(*(SORT(.rti_fn*)))
    __rt_init_end = .;
    . = ALIGN(8);
    __rt_utest_tc_tab_start = .;
    KEEP(*(UtestTcTab))
    __rt_utest_tc_tab_end = .;
    . = ALIGN(8);
    __am_apps_data_start = .;
    *(__am_apps.data*) *(__am_apps.sdata*)
    __am_apps_data_end = .;
    . = ALIGN(8);
    __data_extra_end = .;
    
    /* 原有的.data段内容 */
    *(.data*)
    *(.sdata*)
    . = ALIGN(8);
} > sdram AT> flash

_data_end = .;
_data_load_start = LOADADDR(.data);

/* .bss段同理 */
.bss : {
    . = ALIGN(8);
    _bss_start = .;
    
    /* 在这里插入extra.ld的.bss.extra段 */
    . = ALIGN(8);
    __bss_extra_start = .;
    __am_apps_bss_start = .;
    *(__am_apps.bss*) *(__am_apps.sbss*) *(__am_apps.scommon*)
    __am_apps_bss_end = .;
    . = ALIGN(8);
    __bss_extra_end = .;
    
    /* 原有的.bss段内容 */
    *(.bss*)
    *(.sbss*)
    *(.scommon)
    . = ALIGN(8);
} > sdram AT> flash
  _bss_end = .;

  _stack_top = ALIGN(0x1000);    
  . = _stack_top + 0x8000;
  _stack_pointer = .;
  end = .;
  _end = .;
  _heap_start = ALIGN(0x1000);
   _heap_end = ORIGIN(sdram) + LENGTH(sdram); 
  _sram_stack_pointer = ORIGIN(sram) + LENGTH(sram);
  
}

c