package org.arraipay.arraiapay.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TipoMovimentacaoConverter implements AttributeConverter<TipoMovimentacao, String> {
    @Override public String convertToDatabaseColumn(TipoMovimentacao value) { return EnumDatabaseValues.toDatabase(value); }
    @Override public TipoMovimentacao convertToEntityAttribute(String value) { return EnumDatabaseValues.fromDatabase(value, TipoMovimentacao.class); }
}
