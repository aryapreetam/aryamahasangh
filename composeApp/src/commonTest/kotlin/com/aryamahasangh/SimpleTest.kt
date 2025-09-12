package com.aryamahasangh

import kotlin.test.Test
import kotlin.test.assertTrue

class SimpleTest {
  @Test
  fun testBasicFunctionality() {
    println("executing test")
    assertTrue(true, "Basic test should pass")
  }

  @Test
  fun testBasicFunctionality2() {
    println("executing test 2")
    assertTrue(true, "Basic test 2 should pass")
  }
}
