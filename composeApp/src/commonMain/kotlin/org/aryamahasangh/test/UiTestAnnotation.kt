package org.aryamahasangh.test

/**
 * Annotation to mark UI tests that should be run by the allUiTests task
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class UiTest
