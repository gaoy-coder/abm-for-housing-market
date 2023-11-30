package collectors;

import housing.Model;
import housing.Household;
import housing.MortgageAgreement;

import java.util.ArrayList;

/**************************************************************************************************
 * Class to record mortgage data
 *
 * @author daniel, Adrian Carro
 *
 *************************************************************************************************/
public class CreditSupply {

    //------------------//
    //----- Fields -----//
    //------------------//

    private int                             rollingWindow;              // Size of the window to compute rolling averages of core indicators
    private int                             currentTime;                // Local copy of the current time so as to avoid repeated calls to Model
    private int                             currentIndex;               // Local copy of the (current time % rolling window) so as to avoid repeated calls to Model
    private ArrayList<ArrayList<Double>>    oo_ltv;                     // LTV values of individual OO (FTB + HM) mortgage stored on a rolling basis
    private ArrayList<ArrayList<Double>>    oo_lti;                     // LTI values of individual OO (FTB + HM) mortgage stored on a rolling basis
    private double[]                        btl_ltv_sums;               // Sum of monthly LTV values of BTL mortgage stored on a rolling basis
    private double                          interestRate;               // To record interest rate and give it to recorder object for file writing
    private double[]                        totalCreditStock;           // Total stock of mortgage credit stored on an annual rolling basis
    private double                          totalBTLCredit;             // Buy to let mortgage credit
    private double                          totalOOCredit;              // Owner-occupier mortgage credit
    private double                          netCreditGrowth;            // Rate of change of credit per month as percentage
    private int[]                           nNewMortgagesArray;         // total number of new mortgages stored on a rolling basis
    private int[]                           nNewFTBMortgagesArray;      // Total number of new first time buyer mortgages stored on a rolling basis
    private int[]                           nNewBTLMortgagesArray;      // Total number of new buy-to-let mortgages stored on a rolling basis
    private int                             nNewFTBMortgagesToBTL;      // Total number of new first time buyer mortgages to households with the BTL gene
    private double                          newCreditToHM;              // Total amount (principals) of new home mover mortgages
    private double                          newCreditToFTB;             // Total amount (principals) of new first time buyer mortgages
    private double                          newCreditToBTL;             // Total amount (principals) of new buy-to-let mortgages

    //------------------------//
    //----- Constructors -----//
    //------------------------//

    public CreditSupply(int targetPopulation, int rollingWindow) {
        this.rollingWindow = rollingWindow;
        oo_ltv = new ArrayList<>(this.rollingWindow);
        oo_lti = new ArrayList<>(this.rollingWindow);
        for (int i = 0; i < this.rollingWindow; i++) {
            // Initial capacity designed for efficiency up to around 5% of households getting a new mortgage per month
            oo_ltv.add(new ArrayList<>((int)(targetPopulation * 0.05)));
            oo_lti.add(new ArrayList<>((int)(targetPopulation * 0.05)));
        }
        btl_ltv_sums = new double[this.rollingWindow];
        nNewMortgagesArray = new int[this.rollingWindow];
        nNewFTBMortgagesArray = new int[this.rollingWindow];
        nNewBTLMortgagesArray = new int[this.rollingWindow];
        totalCreditStock = new double[12]; // For 12 months, a year
    }

    //-------------------//
    //----- Methods -----//
    //-------------------//

    public void init() {
        for (int i = 0; i < rollingWindow; i++) {
            oo_ltv.get(i).clear();
            oo_lti.get(i).clear();
            btl_ltv_sums[i] = 0.0;
            nNewMortgagesArray[i] = 0;
            nNewFTBMortgagesArray[i] = 0;
            nNewBTLMortgagesArray[i] = 0;
        }
        totalBTLCredit = 0.0;
        totalOOCredit = 0.0;
        for (int i = 0; i < 12; i++) {
            totalCreditStock[i] = 0.0;
        }
    }

    public void preClearingResetCounters(int currentTime) {
        this.currentTime = currentTime;
        currentIndex = this.currentTime % rollingWindow;
        nNewMortgagesArray[currentIndex] = 0;
        nNewFTBMortgagesArray[currentIndex] = 0;
        nNewBTLMortgagesArray[currentIndex] = 0;
        nNewFTBMortgagesToBTL = 0;
        newCreditToHM = 0.0;
        newCreditToFTB = 0.0;
        newCreditToBTL = 0.0;
        oo_ltv.get(currentIndex).clear();
        oo_lti.get(currentIndex).clear();
        btl_ltv_sums[currentIndex] = 0.0;
    }

    /**
     * Collect information on the total stock of credit at the end of the current time step
     */
    public void postClearingRecord() {
        // Update interest rate
        interestRate = Model.bank.getMortgageInterestRate();
        // Count current total stock of mortgage credit, distinguishing between OO (FTB + HM) and BTL credit
        totalOOCredit = 0.0;
        totalBTLCredit = 0.0;
        for(MortgageAgreement m : Model.bank.mortgages) {
            if(m.isBuyToLet) {
                totalBTLCredit += m.principal;
            } else {
                totalOOCredit += m.principal;
            }
        }
        // Compute net credit growth = current total stock - total stock a year ago) / total stock a year ago
        if (totalCreditStock[currentTime % 12] > 0.0) {
            netCreditGrowth = (totalOOCredit + totalBTLCredit - totalCreditStock[currentTime % 12])
                    / totalCreditStock[currentTime % 12];
        } else {
            netCreditGrowth = 0.0;
        }
        // Finally, store current total stock of credit in the rolling array of values
        totalCreditStock[currentTime % 12] = totalOOCredit + totalBTLCredit;
    }

    /**
     * Record information for a newly issued mortgage
     * @param h Household being awarded the loan
     * @param approval Mortgage agreement
     */
    public void recordLoan(Household h, MortgageAgreement approval) {
        nNewMortgagesArray[currentIndex] += 1;
        if (approval.isFirstTimeBuyer) {
            nNewFTBMortgagesArray[currentIndex] += 1;
            newCreditToFTB += approval.principal; // yang: principal 银行借给买家的原始金额
            oo_ltv.get(currentIndex).add(approval.principal / (approval.principal + approval.downPayment));
            oo_lti.get(currentIndex).add(approval.principal / h.getAnnualGrossEmploymentIncome());
            if (h.behaviour.isPropertyInvestor()) {
                nNewFTBMortgagesToBTL += 1;
            }
        } else if (approval.isBuyToLet) {
            newCreditToBTL += approval.principal;
            btl_ltv_sums[currentIndex] += approval.principal / (approval.principal + approval.downPayment);
            nNewBTLMortgagesArray[currentIndex] += 1;
        } else {
            newCreditToHM += approval.principal;
            oo_ltv.get(currentIndex).add(approval.principal / (approval.principal + approval.downPayment));
            oo_lti.get(currentIndex).add(approval.principal / h.getAnnualGrossEmploymentIncome());
        }
    }

    //----- Getter/setter methods -----//

    ArrayList<ArrayList<Double>> getOO_ltv() { return oo_ltv; }

    ArrayList<ArrayList<Double>> getOO_lti() { return oo_lti; }

    double[] getBTL_ltv_sums() { return btl_ltv_sums; }

    double getInterestRate() { return interestRate; }

    int getnStockMortgages() { return Model.bank.mortgages.size(); }

    int[] getnNewMortgagesArray() { return nNewMortgagesArray; }

    int getnNewFTBMortgages() { return nNewFTBMortgagesArray[currentIndex]; }

    int[] getnNewFTBMortgagesArray() { return nNewFTBMortgagesArray; }

    int getnNewFTBMortgagesToBTL() { return nNewFTBMortgagesToBTL; }

    int getnNewHMMortgages() {
        return nNewMortgagesArray[currentIndex]
                - nNewFTBMortgagesArray[currentIndex]
                - nNewBTLMortgagesArray[currentIndex];
    }

    int getnNewBTLMortgages() { return nNewBTLMortgagesArray[currentIndex]; }

    int[] getnNewBTLMortgagesArray() { return nNewBTLMortgagesArray; }

    double getTotalBTLCredit() { return totalBTLCredit; }

    double getTotalOOCredit() { return totalOOCredit; }

    double getNetCreditGrowth() { return netCreditGrowth; }

    double getNewCreditToFTB() { return newCreditToFTB; }

    double getNewCreditToHM() { return newCreditToHM; }

    double getNewCreditToBTL() { return newCreditToBTL; }

    double getNewCreditTotal() { return newCreditToFTB + newCreditToHM + newCreditToBTL; }
}
