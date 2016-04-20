package grid

import java.io.File
import java.nio.file.{Files, Path}
import java.util.Scanner

case class Manifest(val gsIds: List[Int],
                    val rmIds: List[Int],
                    val userIds: List[Int],
                    val nodesPerRm: Int,
                    val repositoryPath: String)

object Manifest {
  def fromFileName(fileName: String): Manifest = {
    val scanner = new Scanner(new File(fileName))

    val gsIds = scanner.nextLine().split(" ").map(_.toInt).toList
    val rmIds = scanner.nextLine().split(" ").map(_.toInt).toList
    val uIds = scanner.nextLine().split(" ").map(_.toInt).toList

    val nodes = scanner.nextInt

    scanner.nextLine()

    val repositoryPath = scanner.nextLine()

//    val meanTimeToGsCrash = scanner.nextInt
//    val meanGsCrashDuration = scanner.nextInt
//    val meanTimeToRmCrash = scanner.nextInt
//    val meanRmCrashDuration = scanner.nextInt

    Manifest(gsIds, rmIds, uIds, nodes, repositoryPath)
  }
}