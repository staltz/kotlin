annotation class Ann

class CustomDelegate {
    public fun get(thisRef: Any?, prop: PropertyMetadata): String = prop.name
}

<!INAPPLICABLE_FIELD_TARGET!>@field:<!DEBUG_INFO_MISSING_UNRESOLVED!>Ann<!><!>
class SomeClass {

    @field:Ann
    protected val simpleProperty: String = "text"

    @field:[Ann]
    protected val simplePropertyWithAnnotationList: String = "text"

    <!INAPPLICABLE_FIELD_TARGET!>@field:Ann<!>
    protected val delegatedProperty: String by CustomDelegate()

    <!INAPPLICABLE_FIELD_TARGET!>@field:<!DEBUG_INFO_MISSING_UNRESOLVED!>Ann<!><!>
    val propertyWithCustomGetter: Int
        get() = 5

    <!INAPPLICABLE_FIELD_TARGET!>@field:<!DEBUG_INFO_MISSING_UNRESOLVED!>Ann<!><!>
    fun annotationOnFunction(a: Int) = a + 5

    fun anotherFun() {
        <!INAPPLICABLE_FIELD_TARGET!>@field:Ann<!>
        val <!UNUSED_VARIABLE!>localVariable<!> = 5
    }

}