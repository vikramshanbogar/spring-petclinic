terraform {
  required_providers {
    postgresql = {
      source  = "cyrilgdn/postgresql"
      version = "1.20.0"
    }
  }
}

provider "postgresql" {
  host            = aws_db_instance.default.address
  port            = aws_db_instance.default.port
  database        = "postgres"
  username        = aws_db_instance.default.username
  password        = aws_db_instance.default.password
  sslmode         = "require"
  connect_timeout = 15
  superuser       = false
}

resource "postgresql_database" "petclinic" {
  name  = "petclinic"
  owner = postgresql_role.petclinic_user.name

  provisioner "local-exec" {
    command = "PGPASSWORD=postgres psql -h ${aws_db_instance.default.address} -p ${aws_db_instance.default.port} -U postgres -d ${self.name} -c 'GRANT CREATE ON SCHEMA public TO ${postgresql_role.petclinic_user.name};'"
  }
}

resource "postgresql_role" "petclinic_user" {
  name     = "petclinic_user"
  login    = true
  password = "petclinic_password"
}

resource "postgresql_grant" "petclinic_user_grant" {
  database    = postgresql_database.petclinic.name
  role        = postgresql_role.petclinic_user.name
  object_type = "database"
  privileges  = ["ALL"]
}


