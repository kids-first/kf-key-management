@Library(value='kids-first/aws-infra-jenkins-shared-libraries', changelog=false) _
ecs_service_type_1_standard {
    projectName = "kf-key-manager"
    internal_app = "false"
    environments = "dev,qa,prd"
    docker_image_type = "debian"
    dependencies = "ecr"
    create_default_iam_role = "0"
    entrypoint_command = "java -jar /opt/kidsfirst/keys/keys.jar"
    external_config_repo = "false"
    quick_deploy = "true"
    create_additional_internal_alb = "0"
    container_port = "8080"
    health_check_path = "/status"
    additional_ssl_cert_domain_name = "*.kidsfirstdrc.org"
}
