package br.dev.bielsolosos.biscraper.core.abstractfields;

import br.dev.bielsolosos.biscraper.core.enums.AnalysisType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.util.Map;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.DEDUCTION,
    defaultImpl = SimpleAnalisisTypeFields.class
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SimpleAnalisisTypeFields.class, name = "SIMPLE"),
    @JsonSubTypes.Type(value = NotebookAnalysisTypeFields.class, name = "NOTEBOOK")
})
@Data
public abstract class AnalysisTypeFields {

    public abstract AnalysisType getAnalysisType();

    public abstract Map<String, Object> getFields();
}
