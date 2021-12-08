// DO NOT EDIT: generated by TupleEncodingSpec.scala
package scommons.websql.encoding

trait TupleDecoders extends BaseEncodingDsl {

  implicit def tuple2Decoder[T1, T2](
    implicit d1: Decoder[T1], d2: Decoder[T2]
  ): Decoder[(T1, T2)] =
    WebSqlDecoder { (i: Index, r: ResultRow) =>
      (d1(i + 0, r),
        d2(i + 1, r))
    }

  implicit def tuple3Decoder[T1, T2, T3](
    implicit d1: Decoder[T1], d2: Decoder[T2], d3: Decoder[T3]
  ): Decoder[(T1, T2, T3)] =
    WebSqlDecoder { (i: Index, r: ResultRow) =>
      (d1(i + 0, r),
        d2(i + 1, r),
          d3(i + 2, r))
    }

  implicit def tuple4Decoder[T1, T2, T3, T4](
    implicit d1: Decoder[T1], d2: Decoder[T2], d3: Decoder[T3], d4: Decoder[T4]
  ): Decoder[(T1, T2, T3, T4)] =
    WebSqlDecoder { (i: Index, r: ResultRow) =>
      (d1(i + 0, r),
        d2(i + 1, r),
          d3(i + 2, r),
            d4(i + 3, r))
    }

  implicit def tuple5Decoder[T1, T2, T3, T4, T5](
    implicit d1: Decoder[T1], d2: Decoder[T2], d3: Decoder[T3], d4: Decoder[T4], d5: Decoder[T5]
  ): Decoder[(T1, T2, T3, T4, T5)] =
    WebSqlDecoder { (i: Index, r: ResultRow) =>
      (d1(i + 0, r),
        d2(i + 1, r),
          d3(i + 2, r),
            d4(i + 3, r),
              d5(i + 4, r))
    }

  implicit def tuple6Decoder[T1, T2, T3, T4, T5, T6](
    implicit d1: Decoder[T1], d2: Decoder[T2], d3: Decoder[T3], d4: Decoder[T4], d5: Decoder[T5], d6: Decoder[T6]
  ): Decoder[(T1, T2, T3, T4, T5, T6)] =
    WebSqlDecoder { (i: Index, r: ResultRow) =>
      (d1(i + 0, r),
        d2(i + 1, r),
          d3(i + 2, r),
            d4(i + 3, r),
              d5(i + 4, r),
                d6(i + 5, r))
    }

  implicit def tuple7Decoder[T1, T2, T3, T4, T5, T6, T7](
    implicit d1: Decoder[T1], d2: Decoder[T2], d3: Decoder[T3], d4: Decoder[T4], d5: Decoder[T5], d6: Decoder[T6], d7: Decoder[T7]
  ): Decoder[(T1, T2, T3, T4, T5, T6, T7)] =
    WebSqlDecoder { (i: Index, r: ResultRow) =>
      (d1(i + 0, r),
        d2(i + 1, r),
          d3(i + 2, r),
            d4(i + 3, r),
              d5(i + 4, r),
                d6(i + 5, r),
                  d7(i + 6, r))
    }

  implicit def tuple8Decoder[T1, T2, T3, T4, T5, T6, T7, T8](
    implicit d1: Decoder[T1], d2: Decoder[T2], d3: Decoder[T3], d4: Decoder[T4], d5: Decoder[T5], d6: Decoder[T6], d7: Decoder[T7], d8: Decoder[T8]
  ): Decoder[(T1, T2, T3, T4, T5, T6, T7, T8)] =
    WebSqlDecoder { (i: Index, r: ResultRow) =>
      (d1(i + 0, r),
        d2(i + 1, r),
          d3(i + 2, r),
            d4(i + 3, r),
              d5(i + 4, r),
                d6(i + 5, r),
                  d7(i + 6, r),
                    d8(i + 7, r))
    }

  implicit def tuple9Decoder[T1, T2, T3, T4, T5, T6, T7, T8, T9](
    implicit d1: Decoder[T1], d2: Decoder[T2], d3: Decoder[T3], d4: Decoder[T4], d5: Decoder[T5], d6: Decoder[T6], d7: Decoder[T7], d8: Decoder[T8], d9: Decoder[T9]
  ): Decoder[(T1, T2, T3, T4, T5, T6, T7, T8, T9)] =
    WebSqlDecoder { (i: Index, r: ResultRow) =>
      (d1(i + 0, r),
        d2(i + 1, r),
          d3(i + 2, r),
            d4(i + 3, r),
              d5(i + 4, r),
                d6(i + 5, r),
                  d7(i + 6, r),
                    d8(i + 7, r),
                      d9(i + 8, r))
    }

  implicit def tuple10Decoder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10](
    implicit d1: Decoder[T1], d2: Decoder[T2], d3: Decoder[T3], d4: Decoder[T4], d5: Decoder[T5], d6: Decoder[T6], d7: Decoder[T7], d8: Decoder[T8], d9: Decoder[T9], d10: Decoder[T10]
  ): Decoder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10)] =
    WebSqlDecoder { (i: Index, r: ResultRow) =>
      (d1(i + 0, r),
        d2(i + 1, r),
          d3(i + 2, r),
            d4(i + 3, r),
              d5(i + 4, r),
                d6(i + 5, r),
                  d7(i + 6, r),
                    d8(i + 7, r),
                      d9(i + 8, r),
                        d10(i + 9, r))
    }

  implicit def tuple11Decoder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11](
    implicit d1: Decoder[T1], d2: Decoder[T2], d3: Decoder[T3], d4: Decoder[T4], d5: Decoder[T5], d6: Decoder[T6], d7: Decoder[T7], d8: Decoder[T8], d9: Decoder[T9], d10: Decoder[T10], d11: Decoder[T11]
  ): Decoder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11)] =
    WebSqlDecoder { (i: Index, r: ResultRow) =>
      (d1(i + 0, r),
        d2(i + 1, r),
          d3(i + 2, r),
            d4(i + 3, r),
              d5(i + 4, r),
                d6(i + 5, r),
                  d7(i + 6, r),
                    d8(i + 7, r),
                      d9(i + 8, r),
                        d10(i + 9, r),
                          d11(i + 10, r))
    }

  implicit def tuple12Decoder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12](
    implicit d1: Decoder[T1], d2: Decoder[T2], d3: Decoder[T3], d4: Decoder[T4], d5: Decoder[T5], d6: Decoder[T6], d7: Decoder[T7], d8: Decoder[T8], d9: Decoder[T9], d10: Decoder[T10], d11: Decoder[T11], d12: Decoder[T12]
  ): Decoder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12)] =
    WebSqlDecoder { (i: Index, r: ResultRow) =>
      (d1(i + 0, r),
        d2(i + 1, r),
          d3(i + 2, r),
            d4(i + 3, r),
              d5(i + 4, r),
                d6(i + 5, r),
                  d7(i + 6, r),
                    d8(i + 7, r),
                      d9(i + 8, r),
                        d10(i + 9, r),
                          d11(i + 10, r),
                            d12(i + 11, r))
    }

  implicit def tuple13Decoder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13](
    implicit d1: Decoder[T1], d2: Decoder[T2], d3: Decoder[T3], d4: Decoder[T4], d5: Decoder[T5], d6: Decoder[T6], d7: Decoder[T7], d8: Decoder[T8], d9: Decoder[T9], d10: Decoder[T10], d11: Decoder[T11], d12: Decoder[T12], d13: Decoder[T13]
  ): Decoder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13)] =
    WebSqlDecoder { (i: Index, r: ResultRow) =>
      (d1(i + 0, r),
        d2(i + 1, r),
          d3(i + 2, r),
            d4(i + 3, r),
              d5(i + 4, r),
                d6(i + 5, r),
                  d7(i + 6, r),
                    d8(i + 7, r),
                      d9(i + 8, r),
                        d10(i + 9, r),
                          d11(i + 10, r),
                            d12(i + 11, r),
                              d13(i + 12, r))
    }

  implicit def tuple14Decoder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14](
    implicit d1: Decoder[T1], d2: Decoder[T2], d3: Decoder[T3], d4: Decoder[T4], d5: Decoder[T5], d6: Decoder[T6], d7: Decoder[T7], d8: Decoder[T8], d9: Decoder[T9], d10: Decoder[T10], d11: Decoder[T11], d12: Decoder[T12], d13: Decoder[T13], d14: Decoder[T14]
  ): Decoder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14)] =
    WebSqlDecoder { (i: Index, r: ResultRow) =>
      (d1(i + 0, r),
        d2(i + 1, r),
          d3(i + 2, r),
            d4(i + 3, r),
              d5(i + 4, r),
                d6(i + 5, r),
                  d7(i + 6, r),
                    d8(i + 7, r),
                      d9(i + 8, r),
                        d10(i + 9, r),
                          d11(i + 10, r),
                            d12(i + 11, r),
                              d13(i + 12, r),
                                d14(i + 13, r))
    }

  implicit def tuple15Decoder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15](
    implicit d1: Decoder[T1], d2: Decoder[T2], d3: Decoder[T3], d4: Decoder[T4], d5: Decoder[T5], d6: Decoder[T6], d7: Decoder[T7], d8: Decoder[T8], d9: Decoder[T9], d10: Decoder[T10], d11: Decoder[T11], d12: Decoder[T12], d13: Decoder[T13], d14: Decoder[T14], d15: Decoder[T15]
  ): Decoder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15)] =
    WebSqlDecoder { (i: Index, r: ResultRow) =>
      (d1(i + 0, r),
        d2(i + 1, r),
          d3(i + 2, r),
            d4(i + 3, r),
              d5(i + 4, r),
                d6(i + 5, r),
                  d7(i + 6, r),
                    d8(i + 7, r),
                      d9(i + 8, r),
                        d10(i + 9, r),
                          d11(i + 10, r),
                            d12(i + 11, r),
                              d13(i + 12, r),
                                d14(i + 13, r),
                                  d15(i + 14, r))
    }

  implicit def tuple16Decoder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16](
    implicit d1: Decoder[T1], d2: Decoder[T2], d3: Decoder[T3], d4: Decoder[T4], d5: Decoder[T5], d6: Decoder[T6], d7: Decoder[T7], d8: Decoder[T8], d9: Decoder[T9], d10: Decoder[T10], d11: Decoder[T11], d12: Decoder[T12], d13: Decoder[T13], d14: Decoder[T14], d15: Decoder[T15], d16: Decoder[T16]
  ): Decoder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16)] =
    WebSqlDecoder { (i: Index, r: ResultRow) =>
      (d1(i + 0, r),
        d2(i + 1, r),
          d3(i + 2, r),
            d4(i + 3, r),
              d5(i + 4, r),
                d6(i + 5, r),
                  d7(i + 6, r),
                    d8(i + 7, r),
                      d9(i + 8, r),
                        d10(i + 9, r),
                          d11(i + 10, r),
                            d12(i + 11, r),
                              d13(i + 12, r),
                                d14(i + 13, r),
                                  d15(i + 14, r),
                                    d16(i + 15, r))
    }

  implicit def tuple17Decoder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17](
    implicit d1: Decoder[T1], d2: Decoder[T2], d3: Decoder[T3], d4: Decoder[T4], d5: Decoder[T5], d6: Decoder[T6], d7: Decoder[T7], d8: Decoder[T8], d9: Decoder[T9], d10: Decoder[T10], d11: Decoder[T11], d12: Decoder[T12], d13: Decoder[T13], d14: Decoder[T14], d15: Decoder[T15], d16: Decoder[T16], d17: Decoder[T17]
  ): Decoder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17)] =
    WebSqlDecoder { (i: Index, r: ResultRow) =>
      (d1(i + 0, r),
        d2(i + 1, r),
          d3(i + 2, r),
            d4(i + 3, r),
              d5(i + 4, r),
                d6(i + 5, r),
                  d7(i + 6, r),
                    d8(i + 7, r),
                      d9(i + 8, r),
                        d10(i + 9, r),
                          d11(i + 10, r),
                            d12(i + 11, r),
                              d13(i + 12, r),
                                d14(i + 13, r),
                                  d15(i + 14, r),
                                    d16(i + 15, r),
                                      d17(i + 16, r))
    }

  implicit def tuple18Decoder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18](
    implicit d1: Decoder[T1], d2: Decoder[T2], d3: Decoder[T3], d4: Decoder[T4], d5: Decoder[T5], d6: Decoder[T6], d7: Decoder[T7], d8: Decoder[T8], d9: Decoder[T9], d10: Decoder[T10], d11: Decoder[T11], d12: Decoder[T12], d13: Decoder[T13], d14: Decoder[T14], d15: Decoder[T15], d16: Decoder[T16], d17: Decoder[T17], d18: Decoder[T18]
  ): Decoder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18)] =
    WebSqlDecoder { (i: Index, r: ResultRow) =>
      (d1(i + 0, r),
        d2(i + 1, r),
          d3(i + 2, r),
            d4(i + 3, r),
              d5(i + 4, r),
                d6(i + 5, r),
                  d7(i + 6, r),
                    d8(i + 7, r),
                      d9(i + 8, r),
                        d10(i + 9, r),
                          d11(i + 10, r),
                            d12(i + 11, r),
                              d13(i + 12, r),
                                d14(i + 13, r),
                                  d15(i + 14, r),
                                    d16(i + 15, r),
                                      d17(i + 16, r),
                                        d18(i + 17, r))
    }

  implicit def tuple19Decoder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19](
    implicit d1: Decoder[T1], d2: Decoder[T2], d3: Decoder[T3], d4: Decoder[T4], d5: Decoder[T5], d6: Decoder[T6], d7: Decoder[T7], d8: Decoder[T8], d9: Decoder[T9], d10: Decoder[T10], d11: Decoder[T11], d12: Decoder[T12], d13: Decoder[T13], d14: Decoder[T14], d15: Decoder[T15], d16: Decoder[T16], d17: Decoder[T17], d18: Decoder[T18], d19: Decoder[T19]
  ): Decoder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19)] =
    WebSqlDecoder { (i: Index, r: ResultRow) =>
      (d1(i + 0, r),
        d2(i + 1, r),
          d3(i + 2, r),
            d4(i + 3, r),
              d5(i + 4, r),
                d6(i + 5, r),
                  d7(i + 6, r),
                    d8(i + 7, r),
                      d9(i + 8, r),
                        d10(i + 9, r),
                          d11(i + 10, r),
                            d12(i + 11, r),
                              d13(i + 12, r),
                                d14(i + 13, r),
                                  d15(i + 14, r),
                                    d16(i + 15, r),
                                      d17(i + 16, r),
                                        d18(i + 17, r),
                                          d19(i + 18, r))
    }

  implicit def tuple20Decoder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20](
    implicit d1: Decoder[T1], d2: Decoder[T2], d3: Decoder[T3], d4: Decoder[T4], d5: Decoder[T5], d6: Decoder[T6], d7: Decoder[T7], d8: Decoder[T8], d9: Decoder[T9], d10: Decoder[T10], d11: Decoder[T11], d12: Decoder[T12], d13: Decoder[T13], d14: Decoder[T14], d15: Decoder[T15], d16: Decoder[T16], d17: Decoder[T17], d18: Decoder[T18], d19: Decoder[T19], d20: Decoder[T20]
  ): Decoder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20)] =
    WebSqlDecoder { (i: Index, r: ResultRow) =>
      (d1(i + 0, r),
        d2(i + 1, r),
          d3(i + 2, r),
            d4(i + 3, r),
              d5(i + 4, r),
                d6(i + 5, r),
                  d7(i + 6, r),
                    d8(i + 7, r),
                      d9(i + 8, r),
                        d10(i + 9, r),
                          d11(i + 10, r),
                            d12(i + 11, r),
                              d13(i + 12, r),
                                d14(i + 13, r),
                                  d15(i + 14, r),
                                    d16(i + 15, r),
                                      d17(i + 16, r),
                                        d18(i + 17, r),
                                          d19(i + 18, r),
                                            d20(i + 19, r))
    }

  implicit def tuple21Decoder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21](
    implicit d1: Decoder[T1], d2: Decoder[T2], d3: Decoder[T3], d4: Decoder[T4], d5: Decoder[T5], d6: Decoder[T6], d7: Decoder[T7], d8: Decoder[T8], d9: Decoder[T9], d10: Decoder[T10], d11: Decoder[T11], d12: Decoder[T12], d13: Decoder[T13], d14: Decoder[T14], d15: Decoder[T15], d16: Decoder[T16], d17: Decoder[T17], d18: Decoder[T18], d19: Decoder[T19], d20: Decoder[T20], d21: Decoder[T21]
  ): Decoder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21)] =
    WebSqlDecoder { (i: Index, r: ResultRow) =>
      (d1(i + 0, r),
        d2(i + 1, r),
          d3(i + 2, r),
            d4(i + 3, r),
              d5(i + 4, r),
                d6(i + 5, r),
                  d7(i + 6, r),
                    d8(i + 7, r),
                      d9(i + 8, r),
                        d10(i + 9, r),
                          d11(i + 10, r),
                            d12(i + 11, r),
                              d13(i + 12, r),
                                d14(i + 13, r),
                                  d15(i + 14, r),
                                    d16(i + 15, r),
                                      d17(i + 16, r),
                                        d18(i + 17, r),
                                          d19(i + 18, r),
                                            d20(i + 19, r),
                                              d21(i + 20, r))
    }

  implicit def tuple22Decoder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22](
    implicit d1: Decoder[T1], d2: Decoder[T2], d3: Decoder[T3], d4: Decoder[T4], d5: Decoder[T5], d6: Decoder[T6], d7: Decoder[T7], d8: Decoder[T8], d9: Decoder[T9], d10: Decoder[T10], d11: Decoder[T11], d12: Decoder[T12], d13: Decoder[T13], d14: Decoder[T14], d15: Decoder[T15], d16: Decoder[T16], d17: Decoder[T17], d18: Decoder[T18], d19: Decoder[T19], d20: Decoder[T20], d21: Decoder[T21], d22: Decoder[T22]
  ): Decoder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22)] =
    WebSqlDecoder { (i: Index, r: ResultRow) =>
      (d1(i + 0, r),
        d2(i + 1, r),
          d3(i + 2, r),
            d4(i + 3, r),
              d5(i + 4, r),
                d6(i + 5, r),
                  d7(i + 6, r),
                    d8(i + 7, r),
                      d9(i + 8, r),
                        d10(i + 9, r),
                          d11(i + 10, r),
                            d12(i + 11, r),
                              d13(i + 12, r),
                                d14(i + 13, r),
                                  d15(i + 14, r),
                                    d16(i + 15, r),
                                      d17(i + 16, r),
                                        d18(i + 17, r),
                                          d19(i + 18, r),
                                            d20(i + 19, r),
                                              d21(i + 20, r),
                                                d22(i + 21, r))
    }

}
