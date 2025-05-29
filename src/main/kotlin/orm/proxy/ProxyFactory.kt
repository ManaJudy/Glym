package com.mana.glym.orm.proxy

import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy
import java.net.URLClassLoader
import java.util.*
import java.util.concurrent.Callable
import javax.tools.ToolProvider

@Suppress("unchecked_cast")
object ProxyFactory {

    @Suppress("unused")
    fun loadProxy(o: Object, proxyName: String) {
        val proxy = ProxyCallableRegistry.get(proxyName)!!.call()
        if (o.javaClass != proxy.javaClass) throw RuntimeException("Invalid proxy instance type")
        for (field in o.javaClass.declaredFields) {
            field.isAccessible = true
            try {
                field.set(o, field.get(proxy))
            } catch (e: IllegalAccessException) {
                throw RuntimeException("Failed to copy field ${field.name}", e)
            }
        }
    }

    fun <C : Any> createProxy(c: Class<C>, callable: Callable<Any>): C {
        val proxyName = "${c.simpleName}Proxy${UUID.randomUUID().toString().replace("-", "")}"
        ProxyCallableRegistry.register(proxyName, callable)
        val proxyPackage = "com.mana.winter.generated"
        val proxyField = "private boolean isProxyLoaded = false;"
        val proxyLoaderMethod = """
            private void loadProxy() {
                if (!isProxyLoaded) {
                    isProxyLoaded = true;
                    try {
                        ${ProxyFactory::class.java.name}.INSTANCE.loadProxy(this, "$proxyName");
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to load proxy: " + e.getMessage(), e);
                    }
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

    fun <T> createProxyList(callable: Callable<List<T>>): List<T> {
        val proxyId = "ListProxy${UUID.randomUUID().toString().replace("-", "")}"
        ProxyCallableRegistry.register(proxyId, callable as Callable<Any>)
        val listHolder = object {
            @Volatile
            var loadedList: List<T>? = null
            var isLoaded = false
        }
        val handler = InvocationHandler { _, method, args ->
            if (!listHolder.isLoaded) {
                synchronized(listHolder) {
                    if (!listHolder.isLoaded) {
                        try {
                            listHolder.loadedList = ProxyCallableRegistry.get(proxyId)!!.call() as List<T>
                            listHolder.isLoaded = true
                        } catch (e: Exception) {
                            throw RuntimeException("Failed to load proxy list: ${e.message}", e)
                        }
                    }
                }
            }
            method.invoke(listHolder.loadedList, *(args ?: emptyArray()))
        }
        return Proxy.newProxyInstance(
            List::class.java.classLoader,
            arrayOf(List::class.java),
            handler
        ) as List<T>
    }
}
