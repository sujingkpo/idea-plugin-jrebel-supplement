package com.czy.jrebelsupplement

import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.JavaProgramPatcher
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import org.apache.commons.lang.StringUtils
import java.io.File
import java.util.regex.Pattern

class JrebelSupplementPatcher : JavaProgramPatcher() {
    private fun dumpJavaParameters(stringBuilder: StringBuilder, javaParameters: JavaParameters) {
        val args = javaParameters.vmParametersList.array
        stringBuilder.append("Vm Params:\n")
        var programArgs = args
        val var5 = args.size
        var var6 = 0
        while (var6 < var5) {
            val arg = programArgs[var6]
            stringBuilder.append(arg).append("\n")
            ++var6
        }
        programArgs = javaParameters.programParametersList.array
        stringBuilder.append("Program Params:\n")
        val var9 = programArgs
        var6 = programArgs.size
        for (var10 in 0 until var6) {
            val arg = var9[var10]
            stringBuilder.append(arg).append("\n")
        }
    }

    override fun patchJavaParameters(executor: Executor, configuration: RunProfile, javaParameters: JavaParameters) {
        val builder = StringBuilder()
        builder.append("Dump Run/Debug Configuration info ----- start\n")
        builder.append("Current Executor Id:").append(executor.id).append("\n")
        if (DefaultRunExecutor.EXECUTOR_ID != executor.id && DefaultDebugExecutor.EXECUTOR_ID != executor.id) {
            if (hasJRebelArgs(javaParameters)) {
                patch(javaParameters)
            }
        } else if (isJRebelRunner) {
            patch(javaParameters)
        }
        dumpJavaParameters(builder, javaParameters)
        dumpStackTrace(builder)
        builder.append("Dump Run/Debug Configuration info ----- end\n")
        println("patchers")
    }

    private fun patch(javaParameters: JavaParameters) {
        val currentPlugin = currentPlugin
        if (null != currentPlugin) {
            val pluginPath = currentPlugin.pluginPath.toAbsolutePath().toString()
            val jrebelMpPlugin = pluginPath + File.separator + "lib" + File.separator + jrebelMpFileName
            var plugins = javaParameters.vmParametersList.getPropertyValue("rebel.plugins")
            plugins = if (!StringUtils.isEmpty(plugins)) {
                "$plugins,$jrebelMpPlugin"
            } else {
                jrebelMpPlugin
            }
            javaParameters.vmParametersList.addProperty("rebel.plugins", plugins)
        }
    }

    private val currentPlugin: IdeaPluginDescriptor?
        get() = PluginManagerCore.getPlugin(PluginId.getId("jrebel-supplement-extension"))

    private fun hasJRebelArgs(javaParameters: JavaParameters): Boolean {
        val args = javaParameters.vmParametersList.array
        for (str in args) {
            if (str.startsWith("-javaagent:") && (str.endsWith("jrebel.jar") || str.endsWith("jrebel-bootstrap.jar"))) {
                return true
            }
            if (str.startsWith("-agentpath") && JREBEL_NATIVE_AGENT_PATTER.matcher(str).matches()) {
                return true
            }
        }
        return false
    }

    private val isJRebelRunner: Boolean
        get() {
            val maxStackDeep = 10
            val stackTrace = Thread.currentThread().stackTrace
            for ((stackFrameIndex, element) in stackTrace.withIndex()) {
                if (stackFrameIndex > maxStackDeep) {
                    return false
                }
                val clzName = element.className
                if ("com.zeroturnaround.javarebel.idea.plugin.runner.JRebelRunner" == clzName || "com.zeroturnaround.javarebel.idea.plugin.runner.JRebelDebugRunner" == clzName) {
                    return true
                }
            }
            return false
        }

    private fun dumpStackTrace(stringBuilder: StringBuilder) {
        stringBuilder.append("StackTrace:\n")
        val maxStackDeep = 10
        val stackTrace = Thread.currentThread().stackTrace
        var stackFrameIndex = 0
        for (element in stackTrace) {
            if (stackFrameIndex > maxStackDeep) {
                break
            }
            if (stackFrameIndex != 0) {
                stringBuilder.append("\t")
            }
            stringBuilder.append(element.className).append("\n")
            ++stackFrameIndex
        }
    }

    private val jrebelMpFileName: String
        get() = String.format("%s-%s%s", "jrebel-supplement-plugin", "1.0.9", ".jar")

    companion object {
        private val JREBEL_NATIVE_AGENT_PATTER = Pattern.compile(".*(libjrebel|jrebel32\\.dll|jrebel64\\.dll).*")
    }
}
