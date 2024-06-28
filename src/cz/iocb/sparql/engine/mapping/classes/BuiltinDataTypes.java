package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDayTimeDuration;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.expression.Literal;



public class BuiltinDataTypes
{
    private static final String xsdPrefix = "http://www.w3.org/2001/XMLSchema#";
    private static final String rdfPrefix = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    public static final IRI xsdBooleanIri = new IRI(xsdPrefix + "boolean");
    public static final IRI xsdShortIri = new IRI(xsdPrefix + "short");
    public static final IRI xsdLongIri = new IRI(xsdPrefix + "long");
    public static final IRI xsdIntIri = new IRI(xsdPrefix + "int");
    public static final IRI xsdFloatIri = new IRI(xsdPrefix + "float");
    public static final IRI xsdDoubleIri = new IRI(xsdPrefix + "double");
    public static final IRI xsdIntegerIri = new IRI(xsdPrefix + "integer");
    public static final IRI xsdDecimalIri = new IRI(xsdPrefix + "decimal");
    public static final IRI xsdDateTimeIri = new IRI(xsdPrefix + "dateTime");
    public static final IRI xsdDateIri = new IRI(xsdPrefix + "date");
    public static final IRI xsdDayTimeDurationIri = new IRI(xsdPrefix + "dayTimeDuration");
    public static final IRI xsdStringIri = new IRI(xsdPrefix + "string");
    public static final IRI rdfLangStringIri = new IRI(rdfPrefix + "langString");

    public static final DataType xsdBooleanType = new DataType(xsdBoolean, BuiltinDataTypes::parseBoolean);
    public static final DataType xsdShortType = new DataType(xsdShort, BuiltinDataTypes::parseShort);
    public static final DataType xsdIntType = new DataType(xsdInt, BuiltinDataTypes::parseInt);
    public static final DataType xsdLongType = new DataType(xsdLong, BuiltinDataTypes::parseLong);
    public static final DataType xsdIntegerType = new DataType(xsdInteger, BuiltinDataTypes::parseInteger);
    public static final DataType xsdDecimalType = new DataType(xsdDecimal, BuiltinDataTypes::parseDecimal);
    public static final DataType xsdFloatType = new DataType(xsdFloat, BuiltinDataTypes::parseFloat);
    public static final DataType xsdDoubleType = new DataType(xsdDouble, BuiltinDataTypes::parseDouble);
    public static final DataType xsdStringType = new DataType(xsdString, BuiltinDataTypes::parseString);
    public static final DataType xsdDayTimeDurationType = new DataType(xsdDayTimeDuration,
            BuiltinDataTypes::parseDayTimeDuration);

    public static final DataType xsdDateType = new DataType(xsdDate, BuiltinDataTypes::parseDate)
    {
        @Override
        public LiteralClass getResourceClass(Literal literal)
        {
            assert literalClass.getTypeIri().equals(literal.getTypeIri());

            if(literal.getValue() == null)
                return unsupportedLiteral;

            return DateConstantZoneClass.get(DateClass.getZone(literal));
        }
    };

    public static final DataType xsdDateTimeType = new DataType(xsdDateTime, BuiltinDataTypes::parseDateTime)
    {
        @Override
        public LiteralClass getResourceClass(Literal literal)
        {
            assert literalClass.getTypeIri().equals(literal.getTypeIri());

            if(literal.getValue() == null)
                return unsupportedLiteral;

            return DateTimeConstantZoneClass.get(DateTimeClass.getZone(literal));
        }
    };

    public static final DataType rdfLangStringType = new DataType(rdfLangString, BuiltinDataTypes::parseString)
    {
        @Override
        public LiteralClass getResourceClass(Literal literal)
        {
            assert literalClass.getTypeIri().equals(literal.getTypeIri());

            if(literal.getValue() == null)
                return unsupportedLiteral;

            return LangStringConstantTagClass.get(literal.getLanguageTag());
        }
    };


    private static final DateTimeFormatter dateFormatter = new DateTimeFormatterBuilder()//
            .appendValue(ChronoField.YEAR, 4, 19, SignStyle.NORMAL).appendLiteral("-")//
            .appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral("-")//
            .appendValue(ChronoField.DAY_OF_MONTH, 2)//
            .appendOffset("+HH:MM", "Z")//
            .toFormatter();

    private static final LocalDate maxDate = LocalDate.parse("5874898-01-01Z", dateFormatter);
    private static final LocalDate minDate = LocalDate.parse("-4714-11-24Z", dateFormatter);

    private static final String dateWithoutZonePattern = "-?([1-9][0-9]{3,}|0[0-9][0-9][1-9]|0[0-9][1-9][0-9]|0[1-9][0-9][0-9])-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])";
    private static final String timezonePattern = "(Z|(\\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00))?";
    private static final String datePattern = dateWithoutZonePattern + timezonePattern;


    @SuppressWarnings("serial") private static final Map<Long, String> eraSign = new HashMap<Long, String>()
    {
        {
            put(0L, "-");
            put(1L, "");
        }
    };

    private static final DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()//
            .appendText(ChronoField.ERA, eraSign)//
            .appendValue(ChronoField.YEAR_OF_ERA, 4, 19, SignStyle.NEVER).appendLiteral("-")//
            .appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral("-")//
            .appendValue(ChronoField.DAY_OF_MONTH, 2).appendLiteral("T")//
            .appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral(":")//
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral(":")//
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)//
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)//
            .optionalStart().appendOffset("+HH:MM", "Z").optionalEnd()//
            .toFormatter(Locale.ENGLISH);

    private static final OffsetDateTime maxDateTime = OffsetDateTime.parse("294277-01-01T00:00:00Z", dateTimeFormatter);
    private static final OffsetDateTime minDateTime = OffsetDateTime.parse("-4714-11-24T00:00:00Z", dateTimeFormatter);

    private static final LocalDateTime maxLocalDateTime = maxDateTime.toLocalDateTime();
    private static final LocalDateTime minLocalDateTime = minDateTime.toLocalDateTime();

    private static final String timeWithoutZonePattern = "(([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9](\\.[0-9]+)?|(24:00:00(\\.0+)?))";
    private static final String dateTimeWithoutZonePattern = dateWithoutZonePattern + "T" + timeWithoutZonePattern;
    private static final String dateTimePattern = dateTimeWithoutZonePattern + timezonePattern;

    private static final String dayTimeDurationPattern = "-?P((([0-9]+D)(T(([0-9]+H)([0-9]+M)?([0-9]+(\\.[0-9]+)?S)?|([0-9]+M)([0-9]+(\\.[0-9]+)?S)?|([0-9]+(\\.[0-9]+)?S)))?)|(T(([0-9]+H)([0-9]+M)?([0-9]+(\\.[0-9]+)?S)?|([0-9]+M)([0-9]+(\\.[0-9]+)?S)?|([0-9]+(\\.[0-9]+)?S))))";
    private static final String dayTimeDurationSplitPattern = "^-?P(([0-9]+)D)?(T(([0-9]+)H)?(([0-9]+)M)?(([0-9]+(\\.[0-9]{1,6})?)[0-9]*S)?)?$";


    private static Boolean parseBoolean(String s)
    {
        s = s.trim();

        if(s.equalsIgnoreCase("true") || s.equals("1"))
            return true;
        if(s.equalsIgnoreCase("false") || s.equals("0"))
            return false;
        else
            return null;
    }


    private static Short parseShort(String s)
    {
        try
        {
            return Short.parseShort(s.trim());
        }
        catch(NumberFormatException e)
        {
            return null;
        }
    }


    private static Integer parseInt(String s)
    {
        try
        {
            return Integer.parseInt(s.trim());
        }
        catch(NumberFormatException e)
        {
            return null;
        }
    }


    private static Long parseLong(String s)
    {
        try
        {
            return Long.parseLong(s.trim());
        }
        catch(NumberFormatException e)
        {
            return null;
        }
    }


    private static BigInteger parseInteger(String s)
    {
        try
        {
            return new BigInteger(s.trim());
        }
        catch(NumberFormatException e)
        {
            return null;
        }
    }


    private static Float parseFloat(String s)
    {
        // the differences between the format of new Float(s) and xsd:float are  hexadecimal literals and the format
        // of infinity

        try
        {
            s = s.trim();

            if(s.contains("0x") || s.contains("0X"))
                return null;

            s = s.replace("INF", "Infinity");

            return Float.parseFloat(s);
        }
        catch(NumberFormatException e)
        {
            return null;
        }
    }


    private static Double parseDouble(String s)
    {
        // the differences between the format of new Double(s) and xsd:double are hexadecimal literals and the format
        // of infinity

        try
        {
            s = s.trim();

            if(s.contains("0x") || s.contains("0X"))
                return null;

            s = s.replace("INF", "Infinity");

            return Double.parseDouble(s);
        }
        catch(NumberFormatException e)
        {
            return null;
        }
    }


    private static BigDecimal parseDecimal(String s)
    {
        // the format of new BigDecimal(s) accepts exponents, which xsd:decimal doesn't

        try
        {
            s = s.trim();

            if(s.contains("e") || s.contains("E"))
                return null;

            return new BigDecimal(s);
        }
        catch(NumberFormatException e)
        {
            return null;
        }
    }


    private static Object parseDateTime(String s)
    {
        try
        {
            s = s.trim();

            if(!s.matches(dateTimePattern))
                return null;

            s = s.replace("(\\.[0-9]{6})[0-9]*", "$1");

            if(s.matches(dateTimeWithoutZonePattern))
            {
                LocalDateTime date = LocalDateTime.parse(s, dateTimeFormatter);

                if(date.compareTo(maxLocalDateTime) < 0 && date.compareTo(minLocalDateTime) >= 0)
                    return date;
            }
            else
            {
                OffsetDateTime date = OffsetDateTime.parse(s, dateTimeFormatter);

                if(date.compareTo(maxDateTime) < 0 && date.compareTo(minDateTime) >= 0)
                    return date;
            }

            return null;
        }
        catch(DateTimeParseException e)
        {
            return null;
        }
    }


    private static String parseDate(String s)
    {
        try
        {
            s = s.trim();

            String value = s;

            if(!value.matches(datePattern))
                return null;

            if(value.matches(dateWithoutZonePattern))
                value = value + "Z";

            LocalDate date = LocalDate.parse(value, dateFormatter);

            if(date.compareTo(maxDate) >= 0 || date.compareTo(minDate) < 0)
                return null;

            return s.replaceAll("[+-]00:00$", "Z");
        }
        catch(DateTimeParseException e)
        {
            return null;
        }
    }


    private static Long parseDayTimeDuration(String s)
    {
        try
        {
            s = s.trim();

            if(!s.matches(dayTimeDurationPattern))
                return null;

            String[] parts = s.replaceFirst(dayTimeDurationSplitPattern, "0$2#0$5#0$7#0$9").split("#");

            BigDecimal daysUsec = new BigDecimal(parts[0]).multiply(new BigDecimal(24 * 60 * 60 * 1000000l));
            BigDecimal hoursUsec = new BigDecimal(parts[1]).multiply(new BigDecimal(60 * 60 * 1000000l));
            BigDecimal minutesUsec = new BigDecimal(parts[2]).multiply(new BigDecimal(60 * 1000000l));
            BigDecimal secondsUsec = new BigDecimal(parts[3]).multiply(new BigDecimal(1000000l));

            BigDecimal result = daysUsec.add(hoursUsec).add(minutesUsec).add(secondsUsec);

            if(s.charAt(0) == '-')
                result = result.multiply(new BigDecimal(-1));

            return result.longValueExact();
        }
        catch(ArithmeticException e)
        {
            return null;
        }
    }


    private static String parseString(String s)
    {
        return s.replace("\\t", "\t").replace("\\n", "\n").replace("\\r", "\r").replace("\\b", "\b")
                .replace("\\f", "\f").replace("\\\"", "\"").replace("\\'", "\'").replace("\\\\", "\\");
    }
}
