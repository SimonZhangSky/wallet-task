package com.pundix.wallet.task.enums;

import lombok.Getter;

@Getter
public enum ETHCoinTypeEnum {
    // ETH主网 合约地址
//    ETH(null),
//    USDT("0xdAC17F958D2ee523a2206206994597C13D831ec7"),
//    USDC("0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eb48"),
//    DAI("0x6B175474E89094C44Da98b954EedeAC495271d0F"),
//    BUSD("0x4fabb145d64652a948d72533023f6e7a623c7c53"),
//    WBTC("0x2260FAC5E5542a773Aa44fBCfeDf7C193bc2C599"),
//    UNI("0x1f9840a85d5aF5bf1D1762F925BDADdC4201F984"),
//    LINK("0x514910771AF9Ca656af840dff83E8264EcF986CA"),
//    SHIB("0x95aD61b0a150d79219dCF64E1E6Cc01f0B64C4cE"),
//    AAVE("0x7Fc66500c84A76Ad7e9c93437bFc5Ac33E2DDaE9"),
//    CRO("0xA0b73E1FF0B809b1eC08d045eDaB377Ff6B7CDE2"),
//    SNX("0xC011A72400E58ecD99Ee497CF89E3775d4bd732F"),
//    MKR("0x9f8F72aA9304c8B593d555F12eF6589cC3A579A2"),
//    BAT("0x0D8775F648430679A709E98d2b0Cb6250d2887EF"),
//    YFI("0x0bc529c00C6401aEF6D220BE8C6Ea1667F6Ad93e"),
//    ZRX("0xE41d2489571d322189246DaFA5ebDe1F4699F498")

    // 测试网络地址
    Sepolia(null),
    USDT("0x271B34781c76fB06bfc54eD9cfE7c817d89f7759"),
    USDC("0x6F6bB5dADDB05718382A0192B65603492C939f8F"),
    AVAIL("0xa5a871723D0a70CddfF57f938F4C06Fc70632EEc")
    ;

    private final String tokenAddress;

    ETHCoinTypeEnum(String tokenAddress) {
        this.tokenAddress = tokenAddress;
    }

    public static ETHCoinTypeEnum getByCoinType(String coinType) {
        for (ETHCoinTypeEnum ethCoinTypeEnum : ETHCoinTypeEnum.values()) {
            if (ethCoinTypeEnum.name().equalsIgnoreCase(coinType)) {
                return ethCoinTypeEnum;
            }
        }
        return null;
    }
}
