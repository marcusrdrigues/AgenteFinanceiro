package br.com.financecorp.agenteriscos;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Mesmas regras do agente-financeiro. allowEmptyShould: no bootstrap ainda não há classes de
 * domínio e aplicação; as regras passam a morder quando elas chegarem no incremento 6.
 */
class ArquiteturaTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importar() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("br.com.financecorp.agenteriscos");
    }

    @Test
    void dominioNaoDependeDeFrameworkNemDeOutrasCamadas() {
        noClasses().that().resideInAPackage("..dominio..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "java.sql..", "org.a2aproject..",
                        "..aplicacao..", "..infraestrutura..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void aplicacaoNaoDependeDoProtocoloNemDaInfraestrutura() {
        noClasses().that().resideInAPackage("..aplicacao..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.ai..", "org.a2aproject..", "..infraestrutura..")
                .allowEmptyShould(true)
                .check(classes);
    }
}
