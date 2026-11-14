package vn.edu.qnu.st.nambede.classroom.classes;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.RepeatedTest;

class ClassCodeGeneratorTest {

    private final ClassCodeGenerator generator = new ClassCodeGenerator();

    @RepeatedTest(20)
    void codesUseOnlyEasyToReadCharacters() {
        assertThat(generator.next()).hasSize(ClassCodeGenerator.LENGTH)
                .matches("[" + ClassCodeGenerator.ALPHABET + "]+");
    }
}
