package fixture.layering

package controllers {
  object HomeController {
    def index(): Int = 1
  }

  class AdminController
}

package object controllers {
  def controllersHelper(): Int = 1
}

package object actions {
  def actionsHelper(): Int = 1
}

package actions {
  object OtherAction {
    def run(): Int = 1
  }

  trait BaseAction {
    def shared(): Int = 1
  }
}

package repositories {
  object UserRepository {
    def find(): Int = 1
  }

  package daos {
    object UserDao {
      def find(): Int = 1
    }
  }

  package models {
    final case class UserRow(id: Int)
  }
}

package models {
  final case class User(id: Int)
  final case class Account(id: Int)
}

package unlayered {
  object Stray {
    def run(): Int = 1
  }
}
