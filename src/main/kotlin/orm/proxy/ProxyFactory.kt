package com.mana.glym.orm.proxy

import java.io.File
import java.lang.reflect.Modifier
import java.net.URLClassLoader
import java.util.*
import java.util.concurrent.Callable
import javax.tools.ToolProvider

@Suppress("unchecked_cast")
object ProxyFactory {
    fun <C : Any> createLazyProxy(c: Class<C>, callable: Callable<Any>): C {
        val proxyName = "${c.simpleName}Proxy${UUID.randomUUID().toString().replace("-", "")}"
        ProxyCallableRegistry.register(proxyName, callable)
        val proxyPackage = "com.mana.winter.generated"
        val proxyField = "private boolean isProxyLoaded = false;"
        val proxyLoaderMethod = """
            private void loadProxy() {
                if (!isProxyLoaded) {
                    ${ProxyCallableRegistry::class.java.name}.INSTANCE.get("$proxyName").call();
                    isProxyLoaded = true;
                }
            }
        """.trimIndent()
        val proxyMethods = StringBuilder()
        for (method in c.declaredMethods) {
            val modifiers = method.modifiers
            if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers) || Modifier.isPrivate(modifiers))
                continue
            val encapsulation = when {
                Modifier.isPublic(modifiers) -> "public"
                Modifier.isProtected(modifiers) -> "protected"
                else -> ""
            }
            val returnType = method.returnType.name
            val name = method.name
            val paramList = method.parameters.mapIndexed { index, p ->
                "${p.type.name} arg$index"
            }.joinToString(", ")
            val paramNamesOnly = method.parameters.indices.joinToString(", ") { "arg$it" }
            proxyMethods.appendLine("""
                @Override
                $encapsulation $returnType $name($paramList) {
                    loadProxy();
                    ${if (returnType != "void") "return " else ""}super.$name($paramNamesOnly);
                }
            """.trimIndent())
        }
        val proxyCode = """
            package $proxyPackage;
            public class $proxyName extends ${c.name} {
                $proxyField
                $proxyLoaderMethod
                $proxyMethods
            }
        """.trimIndent()
        val file = File("src-gen/$proxyName.java")
        file.writeText(proxyCode)
        val compiler = ToolProvider.getSystemJavaCompiler()
        val result = compiler.run(null, null, null, file.path)
        if (result != 0) throw RuntimeException("Error compiling proxy")
        val classLoader = URLClassLoader.newInstance(arrayOf(File("src-gen").toURI().toURL()))
        val clazz = classLoader.loadClass("$proxyPackage.$proxyName")
        return clazz.getDeclaredConstructor().newInstance() as C
    }
}
