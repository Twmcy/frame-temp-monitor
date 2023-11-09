package com.nakkeez.frametempmonitor.model

import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Utility class for tracking device's CPU temperature by looping through
 * possible sensor paths and then reading and converting the values to Celsius.
 * The paths have been tested with Xiaomi 10T Lite and wrong sensor have
 * been commented out, leaving only those couple of paths remaining that
 * will show the CPU temperature.
 */
class CpuTemperature {

    companion object {
        // The path to CPU sensor is different depending on the device so here are
        // the usual paths
        private val paths = arrayOf(
            // "/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp", // Nothing
            // "/sys/devices/system/cpu/cpu0/cpufreq/FakeShmoo_cpu_temp", // Nothing
            // "/sys/class/thermal/thermal_zone0/temp", // Wrong sensor
            "/sys/class/thermal/thermal_zone1/temp", // CPU temperature
            // "/sys/class/i2c-adapter/i2c-4/4-004c/temperature", // Nothing
            // "/sys/devices/platform/tegra-i2c.3/i2c-4/4-004c/temperature", // Nothing
            // "/sys/devices/platform/omap/omap_temp_sensor.0/temperature", // Nothing
            // "/sys/devices/platform/tegra_tmon/temp1_input", // Nothing
            // "/sys/kernel/debug/tegra_thermal/temp_tj", // Nothing
            // "/sys/devices/platform/s5p-tmu/temperature", // Nothing
            // "/sys/class/hwmon/hwmon0/device/temp1_input", // Nothing
            // "/sys/class/hwmon/hwmon0/temp1_input", // Battery temperature
            // "/sys/class/hwmon/hwmonX/temp1_input", // - Nothing
            "/sys/devices/virtual/thermal/thermal_zone1/temp", // CPU temperature
            // "/sys/devices/virtual/thermal/thermal_zone0/temp", // Static 37 °C?
            // "/sys/class/thermal/thermal_zone3/temp", // Wrong sensor
            // "/sys/class/thermal/thermal_zone4/temp", // Wrong sensor
            // "/sys/devices/platform/s5p-tmu/curr_temp" // Nothing
        )

        /**
         * Loops through the array of possible paths to CPU temperature
         * @return CPU temperature in celsius if correct path is found,
         * otherwise null
         */
        fun getCpuTemperature(): Float? {
            // Go through all the paths and check if there is sensor data
            for (path in paths) {
                try {
                    val process = Runtime.getRuntime().exec("cat $path")
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    // Convert the value from milli-degrees Celsius to Celsius
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
