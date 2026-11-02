DoubleSneakBackpack - source project

Purpose: open the equipped ModularPacks backpack on two sneak toggles within 450 ms.
Uses reflection against ModularPacks 2.7.7 internals; no database access or modification.

Build with Java 21 and Maven:
  mvn package

Output:
  target/double-sneak-backpack-1.0.0.jar

Install only after backing up the server and testing on a copy. Place the built jar in plugins/ beside ModularPacks, then restart.

Note: This is source code, not a compiled/tested plugin jar. Paper 26.3's matching API artifact may need adjusting in pom.xml to the exact Paper API version available from the repository.
