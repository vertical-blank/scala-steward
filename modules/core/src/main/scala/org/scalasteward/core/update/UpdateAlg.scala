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

package org.scalasteward.core.update

import cats.Monad
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import org.scalasteward.core.coursier.CoursierAlg
import org.scalasteward.core.data.{ArtifactId, Dependency, GroupId, Update}
import org.scalasteward.core.repoconfig.RepoConfig
import org.scalasteward.core.util
import org.scalasteward.core.util.Nel

final class UpdateAlg[F[_]](
    implicit
    coursierAlg: CoursierAlg[F],
    filterAlg: FilterAlg[F],
    logger: Logger[F],
    F: Monad[F]
) {
  def findUpdate(dependency: Dependency): F[Option[Update.Single]] =
    for {
      newerVersions0 <- coursierAlg.getNewerVersions(dependency)
      maybeUpdate0 = Nel.fromList(newerVersions0).map { newerVersions1 =>
        dependency.toUpdate.copy(newerVersions = newerVersions1.map(_.value))
      }
      maybeUpdate1 = maybeUpdate0.orElse(UpdateAlg.findUpdateWithNewerGroupId(dependency))
    } yield maybeUpdate1

  def findUpdates(dependencies: List[Dependency], repoConfig: RepoConfig): F[List[Update.Single]] =
    for {
      _ <- logger.info(s"Find updates")
      updates0 <- dependencies.traverseFilter(findUpdate)
      updates1 <- filterAlg.localFilterMany(repoConfig, updates0)
      updates2 = ArtifactId
        .combineCrossNames(Update.Single.artifactId)(updates1)
        .sortBy(u => (u.groupId, u.artifactId))
      _ <- logger.info(util.logger.showUpdates(updates2.widen[Update]))
    } yield updates2
}

object UpdateAlg {
  def isUpdateFor(update: Update, dependency: Dependency): Boolean =
    update.groupId === dependency.groupId &&
      update.currentVersion === dependency.version &&
      update.artifactIds.exists { artifactId =>
        dependency.artifactId.names.forall(artifactId.names.contains_)
      }

  def findUpdateWithNewerGroupId(dependency: Dependency): Option[Update.Single] =
    newerGroupId(dependency.groupId, dependency.artifactId).map {
      case (groupId, version) =>
        dependency.toUpdate.copy(newerGroupId = Some(groupId), newerVersions = Nel.one(version))
    }

  private def newerGroupId(groupId: GroupId, artifactId: ArtifactId): Option[(GroupId, String)] =
    Some((groupId.value, artifactId.name)).collect {
      case ("com.geirsson", "sbt-scalafmt")       => (GroupId("org.scalameta"), "2.0.0")
      case ("com.github.mpilquist", "simulacrum") => (GroupId("org.typelevel"), "1.0.0")
      case ("net.ceedubs", "ficus")               => (GroupId("com.iheart"), "1.3.4")
      case ("org.spire-math", "kind-projector")   => (GroupId("org.typelevel"), "0.10.0")
    }
}
