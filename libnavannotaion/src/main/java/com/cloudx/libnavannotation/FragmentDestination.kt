package com.cloudx.libnavannotation

/**
 * Created by Petterp
 * on 2020-02-02
 * Function:
 */
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS
)
annotation class FragmentDestination(
    val pageUrl: String,
    val needLogin: Boolean = false,
    val asStarter: Boolean = false
)