package com.github.f2janyway.plugintest.template

import com.android.tools.idea.wizard.template.*
import com.android.tools.idea.wizard.template.impl.activities.common.addAllKotlinDependencies
import com.android.tools.idea.wizard.template.impl.defaultPackageNameParameter
import com.github.f2janyway.plugintest.listeners.MyProjectManagerListener.Companion.projectInstance
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.KotlinLanguage

class WizardTemplateProviderImpl : WizardTemplateProvider() {
    override fun getTemplates(): List<Template> {
        return listOf(recyclerActivitySetupTemplate)
    }
}

val recyclerActivitySetupTemplate
    get() = template {
        name = "pluginTest RecyclerView Activity"
        description = "리사이클러뷰 액티비티"
        minApi = 16
        category = Category.Other // Check other categories
        formFactor = FormFactor.Mobile
        screens = listOf(
            WizardUiContext.FragmentGallery, WizardUiContext.MenuEntry,
            WizardUiContext.NewProject, WizardUiContext.NewModule
        )

        val packageNameParam = defaultPackageNameParameter
        val className = stringParameter {
            name = "Class Name"
            default = "Titie"
            help = "액티비티 생성 시 사용"
            constraints = listOf(Constraint.NONEMPTY)
        }

        val activityLayoutName = stringParameter {
            name = "Activity Layout Name"
            default = "Titie"
            help = "액티비티 레이아웃 생성 시 사용"
            constraints = listOf(Constraint.LAYOUT, Constraint.UNIQUE, Constraint.NONEMPTY)
            suggest = { activityToLayout(className.value.toSnakeCase()) }
        }

        widgets(
            TextFieldWidget(className),
            TextFieldWidget(activityLayoutName),
            PackageNameWidget(packageNameParam)
        )

        recipe = { data: TemplateData ->
            mvvmRecyclerActivitySetup(
                data as ModuleTemplateData,
                packageNameParam.value,
                className.value,
                activityLayoutName.value
            )
        }
    }

fun RecipeExecutor.mvvmRecyclerActivitySetup(
    moduleData: ModuleTemplateData,
    packageName: String,
    className: String,
    activityLayoutName: String,
) {
    val (projectData, _, _, manifestOut) = moduleData
    val project = projectInstance ?: run {
        println("projectInstance is null!!")
        return
    }

    addAllKotlinDependencies(moduleData)

    val virtualFiles = ProjectRootManager.getInstance(project).contentSourceRoots
    val virtSrc = virtualFiles.firstOrNull { it.path.contains("app/src/main/java") } ?: run {
        println("virtSrc is null!!")
        return
    }
    val virtRes = virtualFiles.firstOrNull { it.path.contains("app/src/main/res") } ?: run {
        println("virtRes is null!!")
        return
    }
    val directorySrc = PsiManager.getInstance(project).findDirectory(virtSrc) ?: run {
        println("directorySrc is null!!")
        return
    }
    val directoryRes = PsiManager.getInstance(project).findDirectory(virtRes) ?: run {
        println("directoryRes is null!!")
        return
    }

    val activityClass = "${className}Activity".capitalize()
    val adapterClass = "${className}RecyclerAdatper".capitalize()
    val viewHolderClass = "${className}ItemViewHolder".capitalize()
    val viewModelClass = "${className}ViewModel".capitalize()

    //AndroidMenifest 에 추가될 정보
    mergeXml(
        manifestTemplateXml(packageName, "${className}Activity"),
        manifestOut.resolve("AndroidManifest.xml")
    )

    //각각의 파일이 저장 될 위치와 파일명 등 필요한 정보들을 정의한다.
    createRecyclerActivity(packageName, className, activityLayoutName, projectData)
        .save(directorySrc, packageName, "$activityClass.kt")

    createRecyclerAdapter(packageName, className)
        .save(directorySrc, "$packageName.adapter", "$adapterClass.kt")

    createViewHolder(packageName, className)
        .save(directorySrc, "$packageName.viewHolder", "$viewHolderClass.kt")

    createViewModel(packageName, className)
        .save(directorySrc, "$packageName.viewModel", "$viewModelClass.kt")

    createRecyclerActivityLayout(packageName, className)
        .save(directoryRes, "layout", "${activityLayoutName}.xml")

    createViewHolderLayout()
        .save(directoryRes, "layout", "item_${className.toSnakeCase()}.xml")
}

fun String.toSnakeCase() = replace(humps, "_").toLowerCase()
private val humps = "(?<=.)(?=\\p{Upper})".toRegex()

//val defaultPackageNameParameter
//    get() = stringParameter {
//        name = "Package name"
//        visible = { !isNewModule }
//        default = "com.example" //기본 패키지명
//        constraints = listOf(Constraint.PACKAGE)
//        suggest = { packageName }
//    }

fun String.save(srcDir: PsiDirectory, subDirPath: String, fileName: String) {
    try {
        val destDir = subDirPath.split(".").toDir(srcDir)
        val psiFile = PsiFileFactory
            .getInstance(srcDir.project)
            .createFileFromText(fileName, KotlinLanguage.INSTANCE, this)
        destDir.add(psiFile)
    } catch (exc: Exception) {
        exc.printStackTrace()
    }
}

fun List<String>.toDir(srcDir: PsiDirectory): PsiDirectory {
    var result = srcDir
    forEach {
        result = result.findSubdirectory(it) ?: result.createSubdirectory(it)
    }
    return result
}

fun manifestTemplateXml(packageName: String, activityClassName: String) = """
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
xmlns:tools="http://schemas.android.com/tools"
package="net.deali.test">

    <application>
        <activity android:name="$packageName.$activityClassName"/>   
    </application>
</manifest>
        """

fun createRecyclerActivity(
    packageName: String,
    className: String,
    activityLayoutName: String,
    projectData: ProjectTemplateData
) = """
package $packageName
	
import ${projectData.applicationPackage}.R
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
    
class ${className}Activity : AppCompatActivity() {
}
""".trimIndent()

fun createRecyclerAdapter(
    packageName: String,
    className: String
) = """
package $packageName
import androidx.recyclerview.widget.RecyclerView

class ${className}RecyclerAdapter() :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

}

""".trimIndent()

fun createViewHolder(
    packageName: String,
    className: String
) = """
package $packageName
import androidx.recyclerview.widget.RecyclerView
import net.deali.buyer.databinding.Item${className}Binding
    
class ${className}ItemViewHolder(val binding: Item${className}Binding)
 : RecyclerView.ViewHolder(binding.root)
""".trimIndent()

fun createViewModel(
    packageName: String,
    className: String
) = """
package $packageName
import androidx.lifecycle.ViewModel

class ${className}ViewModel() : ViewModel() {

}
""".trimIndent()

fun createRecyclerActivityLayout(
    packageName: String,
    className: String
) = """
    <?xml version="1.0" encoding="utf-8"?>
    <layout xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:app="http://schemas.android.com/apk/res-auto"
        xmlns:bind="http://schemas.android.com/tools"
        xmlns:tools="http://schemas.android.com/tools"
        tools:context="$packageName.${className}Activity">

        <data>

            <variable
                name="viewModel"
                type="$packageName.${className}ViewModel" />

        </data>

        <androidx.constraintlayout.widget.ConstraintLayout
                android:layout_width="match_parent"
                android:layout_height="match_parent">

        </androidx.constraintlayout.widget.ConstraintLayout>

    </layout>
""".trimIndent()

fun createViewHolderLayout() = """
<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:bind="http://schemas.android.com/tools"
    xmlns:tools="http://schemas.android.com/tools">

    <data>
    </data>

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="48dp">

    </androidx.constraintlayout.widget.ConstraintLayout>
</layout>
""".trimIndent()