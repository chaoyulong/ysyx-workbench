set CLK_PORT_NAME clock
if {[info exists env(CLK_PORT_NAME)]} {
  set CLK_PORT_NAME $::env(CLK_PORT_NAME)
} else {
  puts "Warning: Environment CLK_PORT_NAME is not defined. Use $CLK_PORT_NAME by default."
}

set CLK_FREQ_MHZ 500
if {[info exists env(CLK_FREQ_MHZ)]} {
  set CLK_FREQ_MHZ $::env(CLK_FREQ_MHZ)
} else {
  puts "Warning: Environment CLK_FREQ_MHZ is not defined. Use $CLK_FREQ_MHZ MHz by default."
}

set clk_io_pct 0.2
set clk_port [get_ports $CLK_PORT_NAME]
create_clock -name core_clock -period [expr 1000.0 / $CLK_FREQ_MHZ] $clk_port

set_input_transition 0.1 [all_inputs]
# iEDA 的 set_load 不支持 all_outputs 这类命令对象，需要的话用显式端口名：
# set_load 0.05 [get_ports {io_master_arvalid io_master_rready io_master_awvalid io_master_wvalid io_master_bready}]
