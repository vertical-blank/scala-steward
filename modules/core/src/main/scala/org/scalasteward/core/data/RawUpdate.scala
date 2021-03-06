/*
 * Copyright 2018-2019 Scala Steward contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.scalasteward.core.data

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import monocle.Lens
import org.scalasteward.core.util.Nel

final case class RawUpdate(
    dependency: Dependency,
    newerVersions: Nel[String]
) {
  def toUpdate: Update.Single =
    dependency.toUpdate.copy(newerVersions = newerVersions)
}

object RawUpdate {
  val dependency: Lens[RawUpdate, Dependency] =
    Lens[RawUpdate, Dependency](_.dependency)(dependency => _.copy(dependency = dependency))

  implicit val rawUpdateDecoder: Decoder[RawUpdate] =
    deriveDecoder
}
