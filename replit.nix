{ pkgs }: {
    deps = [
      pkgs.elmPackages.elm
      pkgs.nodejs-18_x
        pkgs.graalvm17-ce
        pkgs.maven
        pkgs.replitPackages.jdt-language-server
        pkgs.replitPackages.java-debug
    ];
}