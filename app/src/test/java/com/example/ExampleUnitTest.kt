package com.example

import com.example.model.Adjustments
import com.example.model.CropRatio
import com.example.model.PhotoFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testPhotoFilterEntries() {
    assertEquals(9, PhotoFilter.entries.size)
    assertTrue(PhotoFilter.entries.any { it == PhotoFilter.BLACK_AND_WHITE })
    assertTrue(PhotoFilter.entries.any { it == PhotoFilter.SEPIA })
    assertTrue(PhotoFilter.entries.any { it == PhotoFilter.VINTAGE })
    assertTrue(PhotoFilter.entries.any { it == PhotoFilter.BLUR })
    assertTrue(PhotoFilter.entries.any { it == PhotoFilter.SHARPEN })
    assertTrue(PhotoFilter.entries.any { it == PhotoFilter.INVERT })
    assertTrue(PhotoFilter.entries.any { it == PhotoFilter.BRIGHTNESS })
    assertTrue(PhotoFilter.entries.any { it == PhotoFilter.CONTRAST })
  }

  @Test
  fun testAdjustmentsDefault() {
    val defaultAdj = Adjustments()
    assertTrue(defaultAdj.isDefault)

    val modifiedAdj = defaultAdj.copy(brightness = 20f)
    assertFalse(modifiedAdj.isDefault)
  }

  @Test
  fun testCropRatios() {
    assertEquals(1f, CropRatio.SQUARE.ratioX / CropRatio.SQUARE.ratioY, 0.01f)
    assertEquals(0.8f, CropRatio.PORTRAIT_4_5.ratioX / CropRatio.PORTRAIT_4_5.ratioY, 0.01f)
  }

  @Test
  fun testGalleryItemModelResolution() {
    val sampleItem = com.example.model.GalleryItem(
      id = "sample_test",
      title = "Test Landscape",
      drawableResId = 12345
    )
    assertEquals(12345, sampleItem.model)
    assertEquals("sample_test", sampleItem.id)
    assertEquals("Test Landscape", sampleItem.title)
  }
}

