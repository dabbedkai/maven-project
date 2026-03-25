package com.grades;

public class Grades {
    private String subject;
    private int prelims;
    private int midterms;
    private int finals;

    public Grades(String subject, int prelims, int midterms, int finals) {
        this.subject = subject;
        this.prelims = prelims;
        this.midterms = midterms;
        this.finals = finals;
    }

    public String getSubject() { return subject; }
    public int getPrelims() { return prelims; }
    public int getMidterms() { return midterms; }
    public int getFinals() { return finals; }

    public String toJson() {
        return String.format(
            "  {\n" +
            "    \"subject\": \"%s\",\n" +
            "    \"prelims\": %d,\n" +
            "    \"midterms\": %d,\n" +
            "    \"finals\": %d\n" +
            "  }", 
            subject, prelims, midterms, finals
        );
    }
}