package com.nakkeez.frametempmonitor.model

import java.io.BufferedReader
import java.io.InputStreamReader

class CpuTemperature {

    companion object {
        // The path to CPU sensor is different depending on the device so here
        // an array with some of usual paths
        private val paths = arrayOf(
            "/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp",
            "/sys/devices/system/cpu/cpu0/cpufreq/FakeShmoo_cpu_temp",
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/i2c-adapter/i2c-4/4-004c/temperature",
            "/sys/devices/platform/tegra-i2c.3/i2c-4/4-004c/temperature",
            "/sys/devices/platform/omap/omap_temp_sensor.0/temperature",
            "/sys/devices/platform/tegra_tmon/temp1_input",
            "/sys/kernel/debug/tegra_thermal/temp_tj",
            "/sys/devices/platform/s5p-tmu/temperature",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/class/hwmon/hwmon0/device/temp1_input",
            "/sys/devices/virtual/thermal/thermal_zone1/temp",
            "/sys/devices/virtual/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone3/temp",
            "/sys/class/thermal/thermal_zone4/temp",
            "/sys/class/hwmon/hwmonX/temp1_input",
            "/sys/devices/platform/s5p-tmu/curr_temp"
        )

        fun getCpuTemperature(): Float? {
            // Go through all the paths and check if right sensor is inside
            for (path in paths) {
                try {
                    val process = Runtime.getRuntime().exec("cat $path")
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    // Convert from milli-degrees Celsius to Celsius
                    val temperature = reader.readLine().toFloat() / 1000.0

                    if (temperature > 0) {
                        return temperature.toFloat()
                    }
                } catch (_: Exception) { }
            }
            return null
        }
    }
}
