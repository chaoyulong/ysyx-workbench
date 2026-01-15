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
    *(.data*)
    *(.sdata*)
    . = ALIGN(8);
  }  > sdram AT> flash
  _data_end = .;
  _data_load_start = LOADADDR(.data);

  .bss : {
    . = ALIGN(8);
	  _bss_start = .;
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

