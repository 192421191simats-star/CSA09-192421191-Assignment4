/** Exchange rates shared by all four styles (application logic kept out of the windows). */
final class Rates {
    private Rates() { }
    static double of(String currency) { return currency.equals("USD") ? 0.012 : 0.011; }
}
