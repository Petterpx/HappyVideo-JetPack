package com.cloudx.libnavcompiler

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.cloudx.libnavannotation.ActivityDestination
import com.cloudx.libnavannotation.FragmentDestination
import com.google.auto.service.AutoService
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.StandardLocation
import kotlin.math.abs

/**
 * Created by Petterp
 * on 2020-02-02
 * Function:
 */
@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes(
    "com.cloudx.libnavannotation.FragmentDestination",
    "com.cloudx.libnavannotation.ActivityDestination"
)
class NavProcessor : AbstractProcessor() {

    private lateinit var message: Messager
    private lateinit var filer: Filer
    private lateinit var fos: FileOutputStream
    private lateinit var writer: OutputStreamWriter

    override fun init(processingEnv: ProcessingEnvironment?) {
        super.init(processingEnv)
        message = processingEnv!!.messager
        filer = processingEnv.filer
    }

    companion object {
        private const val OUTPUT_FILE_NAME = "destination.json"
    }

    override fun process(
        annotations: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment?
    ): Boolean {
        roundEnv?.let {
            val fragmentDestination =
                roundEnv.getElementsAnnotatedWith(FragmentDestination::class.java)
            val activityDestination =
                roundEnv.getElementsAnnotatedWith(ActivityDestination::class.java)

            if (fragmentDestination.isNotEmpty() || activityDestination.isNotEmpty()) {
                val destMap = HashMap<String, JSONObject>()
                handleDestination(fragmentDestination, FragmentDestination::class.java, destMap)
                handleDestination(activityDestination, ActivityDestination::class.java, destMap)

                //app/src/main.assets
                val createResource =
                    filer.createResource(StandardLocation.CLASS_OUTPUT, "", OUTPUT_FILE_NAME)
                val resourcePath = createResource.toUri().path
                message.printMessage(Diagnostic.Kind.NOTE, "resourcePath:$resourcePath")

                val appPath = resourcePath.substring(0, resourcePath.indexOf("app") + 4)
                val assetsPath = appPath + "src/main/assets/"

                try {
                    val file = File(assetsPath)
                    //如果不存在则创建
                    if (!file.exists()) {
                        file.mkdirs()
                    }

                    val outPutFile = File(file, OUTPUT_FILE_NAME)
                    if (outPutFile.exists()) {
                        outPutFile.delete()
                    }
                    outPutFile.createNewFile()

                    //
                    val content = JSON.toJSONString(destMap)
                    //kt自动资源管理
                    fos = FileOutputStream(outPutFile)
//                    fos.use { fileOutputStream ->
                    writer = OutputStreamWriter(fos, "UTF-8")
//                        writer.use {
                    writer.write(content)
                    writer.flush()
//                        }
//                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return true
    }

    private fun handleDestination(
        elements: Set<Element>,
        annotationClass: Class<out Annotation>,
        destMap: HashMap<String, JSONObject>
    ) {

        elements.map {
            it as TypeElement
        }.forEach {
            var pageUrl: String? = null
            val id = abs(destMap.hashCode())
            val cazName = it.qualifiedName.toString()
            var needLogin = false
            var asStarter = false
            //区分是Fragment还是Activity
            var isFragment = true
            when (val annotation = it.getAnnotation(annotationClass)) {
                is FragmentDestination -> {
                    pageUrl = annotation.pageUrl
                    asStarter = annotation.asStarter
                    needLogin = annotation.needLogin
                }
                is ActivityDestination -> {
                    pageUrl = annotation.pageUrl
                    asStarter = annotation.asStarter
                    needLogin = annotation.needLogin
                    isFragment = false
                }
            }
            if (destMap.containsKey(pageUrl)) {
                message.printMessage(Diagnostic.Kind.ERROR, "不同的页面不允许使用相同的pageUrl$cazName")
            } else {
                val objects = JSONObject()
                objects["id"] = id
                objects["needLogin"] = needLogin
                objects["asStarter"] = asStarter
                objects["pageUrl"] = pageUrl
                objects["isFragment"] = isFragment
                destMap[pageUrl.toString()] = objects
            }
        }
    }


}