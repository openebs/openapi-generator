with (import <nixpkgs> {});
mkShell {
  buildInputs = [
    maven
    jdk 
  ];
}
