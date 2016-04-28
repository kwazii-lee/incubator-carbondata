/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.carbondata.examples

import java.io.File

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.CarbonContext

import org.carbondata.core.util.CarbonProperties

object CarbonExample {

  def main(args: Array[String]) {

    // get current directory:/examples
    val currentDirectory = new File(this.getClass.getResource("/").getPath + "/../../")
      .getCanonicalPath

    // specify parameters
    val storeLocation = currentDirectory + "/target/store"
    val kettleHome = new File(currentDirectory + "/../processing/carbonplugins").getCanonicalPath
    val hiveMetaPath = currentDirectory + "/target/hivemetadata"
    val testData = currentDirectory + "/src/main/resources/data.csv"

    val sc = new SparkContext(new SparkConf()
      .setAppName("CarbonExample")
      .setMaster("local[2]"))

    val cc = new CarbonContext(sc, storeLocation)

    // As Carbon using kettle, so need to set kettle configuration
    cc.setConf("carbon.kettle.home", kettleHome)
    cc.setConf("hive.metastore.warehouse.dir", hiveMetaPath)

    // whether use table split partition
    // true -> use table split partition, support multiple partition loading
    // false -> use node split partition, support data load by host partition
    CarbonProperties.getInstance().addProperty("carbon.table.split.partition.enable", "false")

    // @TODO need to change the date to date type as direct surrogate is not implemented
    // after implementation need to update the test case
    cc.sql("CREATE CUBE testTableV1 DIMENSIONS (ID Integer, Date String, Country String, " +
      "Name String, Phonetype String, Serialname String) " +
      "MEASURES (Salary Integer) " +
      "OPTIONS (PARTITIONER [PARTITION_COUNT=1])").show

    cc.sql(
      s"LOAD DATA FACT FROM '$testData' INTO CUBE testTableV1 OPTIONS(DELIMITER ',', FILEHEADER" +
        s" '')").show

    cc.sql("select Country,count(salary) from testTableV1 " +
      "where Country in ('china','france') group by Country")
      .show()
  }
}
