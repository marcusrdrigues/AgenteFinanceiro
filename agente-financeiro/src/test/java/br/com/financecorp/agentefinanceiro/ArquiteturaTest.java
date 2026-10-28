package br.com.financecorp.agentefinanceiro;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** A dependência aponta para dentro (constituição II): domínio e aplicação não conhecem framework de IA, banco nem protocolo. */
class ArquiteturaTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importar() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("br.com.financecorp.agentefinanceiro");
    }

    @Test
    void dominioNaoDependeDeFrameworkNemDeOutrasCamadas() {
        noClasses().that().resideInAPackage("..dominio..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "java.sql..",
                        "..aplicacao..", "..infraestrutura..", "..interfaces..")
                .check(classes);
    }

    @Test
    void aplicacaoNaoDependeDeSdkDeIaBancoProtocoloNemDeAdaptadores() {
        noClasses().that().resideInAPackage("..aplicacao..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.ai..", "org.a2aproject..", "java.sql..",
                        "..infraestrutura..", "..interfaces..")
                .check(classes);
    }

    @Test
    void interfacesNaoDependemDaInfraestrutura() {
        noClasses().that().resideInAPackage("..interfaces..")
                .should().dependOnClassesThat().resideInAPackage("..infraestrutura..")
                .check(classes);
    }
}
