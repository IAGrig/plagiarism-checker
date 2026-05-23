package plagiarism.testutils

import com.dimafeng.testcontainers.QdrantContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import org.scalatest.Suite

trait QdrantTestContainer extends TestContainerForAll { self: Suite =>

  override val containerDef: QdrantContainer.Def = QdrantContainer.Def()

}
