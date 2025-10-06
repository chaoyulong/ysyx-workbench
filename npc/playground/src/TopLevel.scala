package playground

import spinal.core._

// Hardware definition
case class TopLevel() extends Component {
  val io = new Bundle {
    val num  = in  port UInt(32 bits)
    val seg = out Vec(Bits(8 bits), 8)
  }
  val seg7Insts = Array.fill(8)(Seg7())
  for (i <- 0 until 8) {
    seg7Insts(i).io.dis_num := io.num(i * 4 to i * 4 + 3)
    io.seg(i) := seg7Insts(i).io.seg_out
  }
}

case class Seg7() extends Component {
  val io = new Bundle {
    val dis_num = in port UInt(4 bits)
    val seg_out = out port Bits(8 bits)
  }

  val seg_out_reg = Reg(Bits(8 bits)) init B"8'b11111111"

  switch(io.dis_num) {
    is(U"4'x0") {seg_out_reg := B"8'b00000011"}
    is(U"4'x1") {seg_out_reg := B"8'b10011111"}
    is(U"4'x2") {seg_out_reg := B"8'b00100101"}
    is(U"4'x3") {seg_out_reg := B"8'b00001101"}
    is(U"4'x4") {seg_out_reg := B"8'b10011001"}
    is(U"4'x5") {seg_out_reg := B"8'b01001001"}
    is(U"4'x6") {seg_out_reg := B"8'b01000001"}
    is(U"4'x7") {seg_out_reg := B"8'b00011111"}
    is(U"4'x8") {seg_out_reg := B"8'b00000001"}
    is(U"4'x9") {seg_out_reg := B"8'b00001001"}
    is(U"4'xa") {seg_out_reg := B"8'b00010001"}
    is(U"4'xb") {seg_out_reg := B"8'b11000001"}
    is(U"4'xc") {seg_out_reg := B"8'b01100011"}
    is(U"4'xd") {seg_out_reg := B"8'b10000101"}
    is(U"4'xe") {seg_out_reg := B"8'b01100001"}
    is(U"4'xf") {seg_out_reg := B"8'b01110001"}
  }

  io.seg_out := seg_out_reg
}