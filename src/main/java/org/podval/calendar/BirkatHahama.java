package org.podval.calendar;


public final class BirkatHahama {

    public final int FIRST = Calendar.getJewish().dateFromDate(1, JewishMonth.Adar, 22).getDays();


    // KH 9:3
    public final long FIRST_TKUFAS_NISSAN = Calendar.getJewish().molad(1, 7)
        - 7*JewishCalendar.PARTS_IN_DAY
        - 9*JewishCalendar.PARTS_IN_HOUR
        - 642;


    public Date<JewishMonth> getDate(final int number) {
        return Calendar.getJewish().dateFromDays(FIRST + number * (28 * 365 + 7));
    }


    public void print() {
        System.out.println("||Cycle||Jewish||Secular||");
        for (int cycle = 0; cycle <= 214; cycle++) {
            final Date<JewishMonth> jDate = getDate(cycle);
            final Date<GregorianMonth> gDate = Calendar.getGregorian().dateFromDays(jDate.getDays());
            System.out.println("||" + cycle + "||" + jDate + "||" + gDate + "||");
        }
    }


    public void tabulate() {
        final int[] adar = new int[30];
        final int[] nissan = new int[31];

        for (int cycle = 0; cycle <= 214; cycle++) {
            final Date<JewishMonth> date = getDate(cycle);
            if ((date.getMonth().month == JewishMonth.Adar) || (date.getMonth().month == JewishMonth.AdarII)) {
                adar[date.getDay()]++;
            } else if (date.getMonth().month == JewishMonth.Nissan) {
                nissan[date.getDay()]++;
            } else {
                throw new Error();
            }
        }

        System.out.println("||Day||Times||Histogram||");
        tabulateMonth("Adar", adar, 10, 29);
        tabulateMonth("Nissan", nissan, 1, 26);
    }


    private void tabulateMonth(final String name, final int[] month, final int from, final int to) {
        for (int day = from; day <= to; day++) {
            final int number = month[day];
            final String stars = (number == 0) ? "" : "*************************".substring(0, number);
            System.out.println("||" + name + " " + day + "||" + number + "||" + stars + "||");
        }
    }


    private void moladTable(final int year) {
        for (int m = 1; m <= Calendar.getJewish().monthsInYear(year); m++) {
            System.out.println("Molad " + year + " " + m + " = " + Calendar.getJewish().moladDate(year, m));
        }
    }


    public static void main(final String[] args) {
//        new BirkatHahama().print();
//        System.out.println();
//        new BirkatHahama().tabulate();
//        System.out.println(new BirkatHahama().FIRST);
//        System.out.println(Calendar.getJewish().dateFromDays(175));
//        System.out.println(Calendar.getJewish().daysFromParts(new BirkatHahama().FIRST_TKUFAS_NISSAN));
//        System.out.println(Calendar.getJewish().dateFromDays(171));
//        System.out.println(Calendar.getJewish().dateFromDays(5));
//        System.out.println(Calendar.getJewish().dateFromDays(4));
//        System.out.println(Calendar.getJewish().dateFromDays(3));

        new BirkatHahama().moladTable(5769);
    }
}
