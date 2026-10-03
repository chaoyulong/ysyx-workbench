import mill._, scalalib._

val spinalVersion = "1.12.3"

object playground extends SbtModule {
  def scalaVersion = "2.13.14"
  override def millSourcePath = os.pwd
  def sources = T.sources(
    millSourcePath / "playground",
    millSourcePath / "playground" / "src",
    millSourcePath / "playground" / "test",
    millSourcePath / "playground" / "formal"
  )
  def ivyDeps = Agg(
    ivy"com.github.spinalhdl::spinalhdl-core:$spinalVersion",
    ivy"com.github.spinalhdl::spinalhdl-lib:$spinalVersion"
  )
  def scalacPluginIvyDeps = Agg(ivy"com.github.spinalhdl::spinalhdl-idsl-plugin:$spinalVersion")
}
