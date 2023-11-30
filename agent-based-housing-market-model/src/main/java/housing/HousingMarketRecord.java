package housing;

import utilities.PriorityQueue2D;

/**************************************************************************************************
 * Root class to encapsulate information on housing market transactions, both offers and bids. Both
 * HouseOfferRecord, with information on the offer/seller, and HouseBidderRecord, with information
 * on the bid/bidder, will extend this class. Notably, the comparators for the ordering of both
 * priority queues (price-quality, price-yield) are implemented here.
 *
 * @author daniel, Adrian Carro
 *
 *************************************************************************************************/
public abstract class HousingMarketRecord {

    //------------------//
    //----- Fields -----//
    //------------------//

    private double price;
    private int id;  // In order to get a unique, repeatable ordering
    private static int id_pool = 0;

    //------------------------//
    //----- Constructors -----//
    //------------------------//

    HousingMarketRecord(double price) {
        this.price = price;
        id = id_pool++;
    }

    //----------------------//
    //----- Subclasses -----//
    //----------------------//

    /**
     * Class that implements the comparators needed for inserting HousingMarketRecord objects into PriorityQueue2D. In
     * particular, this class implements the comparators for a price-quality priority queue.
     */
    public static class PQComparator implements PriorityQueue2D.XYComparator<HousingMarketRecord> {

        /**
         * @return -1 or 1 if arg0 is, respectively, cheaper than or more expensive than arg1 solving the arg0 == arg1
         * case by reverse comparing their qualities and comparing their Id's if they also have the same quality
         */
        @Override
        public int XYCompare(HousingMarketRecord arg0, HousingMarketRecord arg1) {
            double diff = arg0.price - arg1.price;
            if (diff == 0.0) {
                diff = arg1.getQuality() - arg0.getQuality(); // Note the reverse ordering here
                if (diff == 0.0) {
                    diff = arg0.getId() - arg1.getId();
                }
            }
            return (int) Math.signum(diff);
        }

        /**
         * @return -1, 0 or 1 if arg0 is, respectively, cheaper than, as expensive as, or more expensive than arg1
         */
        @Override
        public int XCompare(HousingMarketRecord arg0, HousingMarketRecord arg1) {
            return (int) Math.signum(arg0.price - arg1.price);
        }

        /**
         * @return -1, 0 or 1 if arg0 has, respectively, less quality than, equal quality as, or greater quality than
         * arg1
         */
        @Override
        public int YCompare(HousingMarketRecord arg0, HousingMarketRecord arg1) {
            return Integer.signum(arg0.getQuality() - arg1.getQuality());
        }
    }

    /**
     * Class that implements the comparators needed for inserting HousingMarketRecord objects into PriorityQueue2D. In
     * particular, this class implements the comparators for a price-yield priority queue.
     */
    public static class PYComparator implements PriorityQueue2D.XYComparator<HousingMarketRecord> {

        /**
         * @return -1 or 1 if arg0 is, respectively, cheaper than or more expensive than arg1 solving the arg0 == arg1
         * case by reverse comparing their yields and comparing their Id's if they also have the same quality
         */
        @Override
        public int XYCompare(HousingMarketRecord arg0, HousingMarketRecord arg1) {
            double diff = arg0.price - arg1.price;
            if (diff == 0.0) {
                diff = arg1.getYield() - arg0.getYield(); // Note the reverse ordering here
                if (diff == 0.0) {
                    diff = arg0.getId() - arg1.getId();
                }
            }
            return (int) Math.signum(diff);
        }

        /**
         * @return -1, 0 or 1 if arg0 is, respectively, cheaper than, as expensive as, or more expensive than arg1
         */
        @Override
        public int XCompare(HousingMarketRecord arg0, HousingMarketRecord arg1) {
            return (int) Math.signum(arg0.price - arg1.price);
        }

        /**
         * @return -1, 0 or 1 if arg0 has, respectively, a smaller yield than, the same yield as, or a higher yield than
         * arg1
         */
        @Override
        public int YCompare(HousingMarketRecord arg0, HousingMarketRecord arg1) {
            return (int) Math.signum(arg0.getYield() - arg1.getYield());
        }
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

    //----- Getter/setter methods -----//

    // TODO: Check if we really need this to be abstract, or even to stay here
    public abstract int getQuality();

    // Dummy method so that it can be overridden at HouseOfferRecord
    public double getYield() {
        System.out.println("Strange: The program shouldn't have entered here!");
        return 0.0;
    }

    public int getId() { return id; }

    public double getPrice() { return price; }

    public void setPrice(double newPrice) { price = newPrice; }
}
