package com.blockchain.unifiedcryptowallet.data.activity.repository

import com.blockchain.api.selfcustody.activity.activityDetailSerializer
import com.blockchain.api.selfcustody.activity.activityIconSerializer
import com.blockchain.api.selfcustody.activity.activityViewItemSerializer
import com.blockchain.api.selfcustody.activity.stackComponentSerializer
import com.blockchain.data.DataResource
import com.blockchain.serializers.BigDecimalSerializer
import com.blockchain.serializers.BigIntSerializer
import com.blockchain.serializers.IsoDateSerializer
import com.blockchain.serializers.KZonedDateTimeSerializer
import com.blockchain.unifiedcryptowallet.data.activity.datasource.UnifiedActivityStore
import com.blockchain.unifiedcryptowallet.domain.activity.model.UnifiedActivityItem
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.Test
import java.util.Calendar
import kotlin.test.assertTrue

class UnifiedActivityRepositoryTest {
    val unifiedActivityStore: UnifiedActivityStore = mockk()
    val unifiedActivityService = UnifiedActivityRepository(unifiedActivityStore, mockk())

    private val json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        serializersModule = SerializersModule {
            contextual(BigDecimalSerializer)
            contextual(BigIntSerializer)
            contextual(IsoDateSerializer)
            contextual(KZonedDateTimeSerializer)
            stackComponentSerializer()
            activityViewItemSerializer()
            activityIconSerializer()
            activityDetailSerializer()
        }
    }

    @Test
    fun tt() = runTest {
        coEvery { unifiedActivityStore.stream(any()) } returns flowOf(DataResource.Data(json.decodeFromString(js)))

        val response = unifiedActivityService
            .activityForAccount("", "", "", "", "").first()

        assert(response is DataResource.Data)

        with((response as DataResource.Data).data) {
            val activity: List<UnifiedActivityItem> = this.activity

            assertTrue { true }

            activity.groupBy {
                it.date?.let {
                    Calendar.getInstance().apply {
                        set(Calendar.MONTH, it.get(Calendar.MONTH))
                        set(Calendar.YEAR, it.get(Calendar.YEAR))
                    }.let {
                        "${it.get(Calendar.MONTH)} ${it.get(Calendar.YEAR)}"
                    }
                }?: "pending"
            }

            assertTrue { true }
        }
    }

    val js = """{
  "activity": [
    {
      "id": "0x462395e2069a2c726bfd0bf509cea659c9e39ca6a7fce23311ebdf3420a4955e",
      "externalUrl": "https://polygonscan.com/tx/0x462395e2069a2c726bfd0bf509cea659c9e39ca6a7fce23311ebdf3420a4955e",
      "item": {
        "leadingImage": {
          "main": "https://login.blockchain.com/static/asset/icon/send.svg",
          "tag": "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/polygon/info/logo.png",
          "type": "SMALL_TAG"
        },
        "leading": [
          {
            "value": "Sent MATIC",
            "style": {
              "style": "paragraph2",
              "color": "text-title"
            },
            "type": "TEXT"
          },
          {
            "value": "3 Oct 2022",
            "style": {
              "style": "caption1",
              "color": "text-body"
            },
            "type": "TEXT"
          }
        ],
        "trailing": [
          {
            "value": "0.02 USD",
            "style": {
              "style": "paragraph2",
              "color": "text-title"
            },
            "type": "TEXT"
          },
          {
            "value": "0.0302315559931 MATIC",
            "style": {
              "style": "caption1",
              "color": "text-body"
            },
            "type": "TEXT"
          }
        ],
        "type": "STACK_VIEW"
      },
      "detail": {
        "title": "Sent MATIC",
        "subtitle": "0.0302315559931 MATIC",
        "icon": {
          "main": "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/polygon/info/logo.png",
          "tag": "https://login.blockchain.com/static/asset/icon/send.svg",
          "type": "SMALL_TAG"
        },
        "itemGroups": [
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Amount",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0.0302315559931 MATIC",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  },
                  {
                    "value": "0.02 USD",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "From",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x6a4e9313512962a9d4ebd7348cfcf7d0ee18a2ae",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "To",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x3a0ff2a08e470ccfbcef9a7de79a2439f3ccb54e",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              }
            ]
          },
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Network fee",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0.000630000000483 MATIC",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  },
                  {
                    "value": "0.00 USD",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Status",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "Completed",
                    "style": "successBadge",
                    "type": "BADGE"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Time",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "3 Oct 2022, 09:01",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Transaction ID",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x462395e2069a2c726bfd0bf509cea659c9e39ca6a7fce23311ebdf3420a4955e",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "text": "Copy Transaction ID",
                "style": "secondaryButton",
                "actionType": "COPY",
                "actionData": "0x462395e2069a2c726bfd0bf509cea659c9e39ca6a7fce23311ebdf3420a4955e",
                "type": "BUTTON"
              }
            ]
          }
        ],
        "floatingActions": [
          {
            "text": "View on Explorer",
            "style": "primary",
            "actionType": "OPEN_URL",
            "actionData": "https://polygonscan.com/tx/0x462395e2069a2c726bfd0bf509cea659c9e39ca6a7fce23311ebdf3420a4955e",
            "type": "BUTTON"
          }
        ],
        "type": "GROUPED_ITEMS"
      },
      "state": "COMPLETED",
      "timestamp": null
    },
    {
      "id": "0x1b4a00ac459b13396b53ce79df414091096eee84e066d7021843fd4fe5c1e5a7",
      "externalUrl": "https://polygonscan.com/tx/0x1b4a00ac459b13396b53ce79df414091096eee84e066d7021843fd4fe5c1e5a7",
      "item": {
        "leadingImage": {
          "main": "https://login.blockchain.com/static/asset/icon/send.svg",
          "tag": "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/polygon/info/logo.png",
          "type": "SMALL_TAG"
        },
        "leading": [
          {
            "value": "Sent MATIC",
            "style": {
              "style": "paragraph2",
              "color": "text-title"
            },
            "type": "TEXT"
          },
          {
            "value": "12 Sep 2022",
            "style": {
              "style": "caption1",
              "color": "text-body"
            },
            "type": "TEXT"
          }
        ],
        "trailing": [
          {
            "value": "0.00 USD",
            "style": {
              "style": "paragraph2",
              "color": "text-title"
            },
            "type": "TEXT"
          },
          {
            "value": "0.002 MATIC",
            "style": {
              "style": "caption1",
              "color": "text-body"
            },
            "type": "TEXT"
          }
        ],
        "type": "STACK_VIEW"
      },
      "detail": {
        "title": "Sent MATIC",
        "subtitle": "0.002 MATIC",
        "icon": {
          "main": "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/polygon/info/logo.png",
          "tag": "https://login.blockchain.com/static/asset/icon/send.svg",
          "type": "SMALL_TAG"
        },
        "itemGroups": [
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Amount",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0.002 MATIC",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  },
                  {
                    "value": "0.00 USD",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "From",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x6a4e9313512962a9d4ebd7348cfcf7d0ee18a2ae",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "To",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x4bf0a012dce82cc8e82ee47d47457b61fa468f7d",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              }
            ]
          },
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Network fee",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0.00147 MATIC",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  },
                  {
                    "value": "0.00 USD",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Status",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "Completed",
                    "style": "successBadge",
                    "type": "BADGE"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Time",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "12 Sep 2022, 10:28",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Transaction ID",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x1b4a00ac459b13396b53ce79df414091096eee84e066d7021843fd4fe5c1e5a7",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "text": "Copy Transaction ID",
                "style": "secondaryButton",
                "actionType": "COPY",
                "actionData": "0x1b4a00ac459b13396b53ce79df414091096eee84e066d7021843fd4fe5c1e5a7",
                "type": "BUTTON"
              }
            ]
          }
        ],
        "floatingActions": [
          {
            "text": "View on Explorer",
            "style": "primary",
            "actionType": "OPEN_URL",
            "actionData": "https://polygonscan.com/tx/0x1b4a00ac459b13396b53ce79df414091096eee84e066d7021843fd4fe5c1e5a7",
            "type": "BUTTON"
          }
        ],
        "type": "GROUPED_ITEMS"
      },
      "state": "COMPLETED",
      "timestamp": null
    },
    {
      "id": "0xe6289e92c02fb383f5fdd722dcdd1541b1b23b2c06baa2a6e59840c55b1849e3",
      "externalUrl": "https://polygonscan.com/tx/0xe6289e92c02fb383f5fdd722dcdd1541b1b23b2c06baa2a6e59840c55b1849e3",
      "item": {
        "leadingImage": {
          "url": "https://login.blockchain.com/static/asset/icon/signature.svg",
          "type": "SINGLE_ICON"
        },
        "leading": [
          {
            "value": "Contract Interaction",
            "style": {
              "style": "paragraph2",
              "color": "text-title"
            },
            "type": "TEXT"
          },
          {
            "value": "15 Aug 2022",
            "style": {
              "style": "caption1",
              "color": "text-body"
            },
            "type": "TEXT"
          }
        ],
        "trailing": [],
        "type": "STACK_VIEW"
      },
      "detail": {
        "title": "Contract Interaction",
        "subtitle": "",
        "icon": {
          "url": "https://login.blockchain.com/static/asset/icon/signature.svg",
          "type": "SINGLE_ICON"
        },
        "itemGroups": [
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Contract Address",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x68c929e7b8fb06c58494a369f6f088fff28f7c77",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              }
            ]
          },
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Network fee",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0.291336 MATIC",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  },
                  {
                    "value": "0.28 USD",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Status",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "Completed",
                    "style": "successBadge",
                    "type": "BADGE"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Time",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "15 Aug 2022, 11:18",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Transaction ID",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0xe6289e92c02fb383f5fdd722dcdd1541b1b23b2c06baa2a6e59840c55b1849e3",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "text": "Copy Transaction ID",
                "style": "secondaryButton",
                "actionType": "COPY",
                "actionData": "0xe6289e92c02fb383f5fdd722dcdd1541b1b23b2c06baa2a6e59840c55b1849e3",
                "type": "BUTTON"
              }
            ]
          }
        ],
        "floatingActions": [
          {
            "text": "View on Explorer",
            "style": "primary",
            "actionType": "OPEN_URL",
            "actionData": "https://polygonscan.com/tx/0xe6289e92c02fb383f5fdd722dcdd1541b1b23b2c06baa2a6e59840c55b1849e3",
            "type": "BUTTON"
          }
        ],
        "type": "GROUPED_ITEMS"
      },
      "state": "COMPLETED",
      "timestamp": 1660562295
    },
    {
      "id": "0xd125e90a5d00aba2d7d29bdeb36e0a85772432899dfbe4b1a294bd8163ad4973",
      "externalUrl": "https://polygonscan.com/tx/0xd125e90a5d00aba2d7d29bdeb36e0a85772432899dfbe4b1a294bd8163ad4973",
      "item": {
        "leadingImage": {
          "main": "https://login.blockchain.com/static/asset/icon/send.svg",
          "tag": "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/polygon/info/logo.png",
          "type": "SMALL_TAG"
        },
        "leading": [
          {
            "value": "Sent MATIC",
            "style": {
              "style": "paragraph2",
              "color": "text-title"
            },
            "type": "TEXT"
          },
          {
            "value": "3 Aug 2022",
            "style": {
              "style": "caption1",
              "color": "text-body"
            },
            "type": "TEXT"
          }
        ],
        "trailing": [
          {
            "value": "0.01 USD",
            "style": {
              "style": "paragraph2",
              "color": "text-title"
            },
            "type": "TEXT"
          },
          {
            "value": "0.01 MATIC",
            "style": {
              "style": "caption1",
              "color": "text-body"
            },
            "type": "TEXT"
          }
        ],
        "type": "STACK_VIEW"
      },
      "detail": {
        "title": "Sent MATIC",
        "subtitle": "0.01 MATIC",
        "icon": {
          "main": "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/polygon/info/logo.png",
          "tag": "https://login.blockchain.com/static/asset/icon/send.svg",
          "type": "SMALL_TAG"
        },
        "itemGroups": [
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Amount",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0.01 MATIC",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  },
                  {
                    "value": "0.01 USD",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "From",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x6a4e9313512962a9d4ebd7348cfcf7d0ee18a2ae",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "To",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x4bf0a012dce82cc8e82ee47d47457b61fa468f7d",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              }
            ]
          },
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Network fee",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0.00063 MATIC",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  },
                  {
                    "value": "0.00 USD",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Status",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "Completed",
                    "style": "successBadge",
                    "type": "BADGE"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Time",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "3 Aug 2022, 18:43",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Transaction ID",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0xd125e90a5d00aba2d7d29bdeb36e0a85772432899dfbe4b1a294bd8163ad4973",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "text": "Copy Transaction ID",
                "style": "secondaryButton",
                "actionType": "COPY",
                "actionData": "0xd125e90a5d00aba2d7d29bdeb36e0a85772432899dfbe4b1a294bd8163ad4973",
                "type": "BUTTON"
              }
            ]
          }
        ],
        "floatingActions": [
          {
            "text": "View on Explorer",
            "style": "primary",
            "actionType": "OPEN_URL",
            "actionData": "https://polygonscan.com/tx/0xd125e90a5d00aba2d7d29bdeb36e0a85772432899dfbe4b1a294bd8163ad4973",
            "type": "BUTTON"
          }
        ],
        "type": "GROUPED_ITEMS"
      },
      "state": "COMPLETED",
      "timestamp": 1659552236
    },
    {
      "id": "0xb98ff924d0f76f046880e2b1330ef98490301a78e84e95f493f5c8151c075aa3",
      "externalUrl": "https://polygonscan.com/tx/0xb98ff924d0f76f046880e2b1330ef98490301a78e84e95f493f5c8151c075aa3",
      "item": {
        "leadingImage": {
          "url": "https://login.blockchain.com/static/asset/icon/signature.svg",
          "type": "SINGLE_ICON"
        },
        "leading": [
          {
            "value": "Contract Interaction",
            "style": {
              "style": "paragraph2",
              "color": "text-title"
            },
            "type": "TEXT"
          },
          {
            "value": "3 Aug 2022",
            "style": {
              "style": "caption1",
              "color": "text-body"
            },
            "type": "TEXT"
          }
        ],
        "trailing": [],
        "type": "STACK_VIEW"
      },
      "detail": {
        "title": "Contract Interaction",
        "subtitle": "",
        "icon": {
          "url": "https://login.blockchain.com/static/asset/icon/signature.svg",
          "type": "SINGLE_ICON"
        },
        "itemGroups": [
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Contract Address",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x2791bca1f2de4661ed88a30c99a7a9449aa84174",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              }
            ]
          },
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Network fee",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0.00124884 MATIC",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  },
                  {
                    "value": "0.00 USD",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Status",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "Completed",
                    "style": "successBadge",
                    "type": "BADGE"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Time",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "3 Aug 2022, 17:16",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Transaction ID",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0xb98ff924d0f76f046880e2b1330ef98490301a78e84e95f493f5c8151c075aa3",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "text": "Copy Transaction ID",
                "style": "secondaryButton",
                "actionType": "COPY",
                "actionData": "0xb98ff924d0f76f046880e2b1330ef98490301a78e84e95f493f5c8151c075aa3",
                "type": "BUTTON"
              }
            ]
          }
        ],
        "floatingActions": [
          {
            "text": "View on Explorer",
            "style": "primary",
            "actionType": "OPEN_URL",
            "actionData": "https://polygonscan.com/tx/0xb98ff924d0f76f046880e2b1330ef98490301a78e84e95f493f5c8151c075aa3",
            "type": "BUTTON"
          }
        ],
        "type": "GROUPED_ITEMS"
      },
      "state": "COMPLETED",
      "timestamp": 1659546972
    },
    {
      "id": "0x3dfe71bc71f3c8e57040761cec6c30aaa9951374867ba797db864cab36b880df",
      "externalUrl": "https://polygonscan.com/tx/0x3dfe71bc71f3c8e57040761cec6c30aaa9951374867ba797db864cab36b880df",
      "item": {
        "leadingImage": {
          "url": "https://login.blockchain.com/static/asset/icon/signature.svg",
          "type": "SINGLE_ICON"
        },
        "leading": [
          {
            "value": "Contract Interaction",
            "style": {
              "style": "paragraph2",
              "color": "text-title"
            },
            "type": "TEXT"
          },
          {
            "value": "16 May 2022",
            "style": {
              "style": "caption1",
              "color": "text-body"
            },
            "type": "TEXT"
          }
        ],
        "trailing": [],
        "type": "STACK_VIEW"
      },
      "detail": {
        "title": "Contract Interaction",
        "subtitle": "",
        "icon": {
          "url": "https://login.blockchain.com/static/asset/icon/signature.svg",
          "type": "SINGLE_ICON"
        },
        "itemGroups": [
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Contract Address",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0xf31cdb090d1d4b86a7af42b62dc5144be8e42906",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              }
            ]
          },
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Network fee",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0.391442222081190234 MATIC",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  },
                  {
                    "value": "0.27 USD",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Status",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "Completed",
                    "style": "successBadge",
                    "type": "BADGE"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Time",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "16 May 2022, 20:46",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Transaction ID",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x3dfe71bc71f3c8e57040761cec6c30aaa9951374867ba797db864cab36b880df",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "text": "Copy Transaction ID",
                "style": "secondaryButton",
                "actionType": "COPY",
                "actionData": "0x3dfe71bc71f3c8e57040761cec6c30aaa9951374867ba797db864cab36b880df",
                "type": "BUTTON"
              }
            ]
          }
        ],
        "floatingActions": [
          {
            "text": "View on Explorer",
            "style": "primary",
            "actionType": "OPEN_URL",
            "actionData": "https://polygonscan.com/tx/0x3dfe71bc71f3c8e57040761cec6c30aaa9951374867ba797db864cab36b880df",
            "type": "BUTTON"
          }
        ],
        "type": "GROUPED_ITEMS"
      },
      "state": "COMPLETED",
      "timestamp": 1652733991
    },
    {
      "id": "0x5f1e2506dec52d84c85b3510275b64392270c1c976dbafd8906a220e35cfc7cd",
      "externalUrl": "https://polygonscan.com/tx/0x5f1e2506dec52d84c85b3510275b64392270c1c976dbafd8906a220e35cfc7cd",
      "item": {
        "leadingImage": {
          "main": "https://login.blockchain.com/static/asset/icon/send.svg",
          "tag": "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/polygon/info/logo.png",
          "type": "SMALL_TAG"
        },
        "leading": [
          {
            "value": "Sent MATIC",
            "style": {
              "style": "paragraph2",
              "color": "text-title"
            },
            "type": "TEXT"
          },
          {
            "value": "13 May 2022",
            "style": {
              "style": "caption1",
              "color": "text-body"
            },
            "type": "TEXT"
          }
        ],
        "trailing": [
          {
            "value": "0.01 USD",
            "style": {
              "style": "paragraph2",
              "color": "text-title"
            },
            "type": "TEXT"
          },
          {
            "value": "0.01 MATIC",
            "style": {
              "style": "caption1",
              "color": "text-body"
            },
            "type": "TEXT"
          }
        ],
        "type": "STACK_VIEW"
      },
      "detail": {
        "title": "Sent MATIC",
        "subtitle": "0.01 MATIC",
        "icon": {
          "main": "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/polygon/info/logo.png",
          "tag": "https://login.blockchain.com/static/asset/icon/send.svg",
          "type": "SMALL_TAG"
        },
        "itemGroups": [
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Amount",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0.01 MATIC",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  },
                  {
                    "value": "0.01 USD",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "From",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x6a4e9313512962a9d4ebd7348cfcf7d0ee18a2ae",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "To",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0xdd6f9fb5aa37f2eb9c5bb1cd01c7e0a29caa605e",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              }
            ]
          },
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Network fee",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0.001953 MATIC",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  },
                  {
                    "value": "0.00 USD",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Status",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "Completed",
                    "style": "successBadge",
                    "type": "BADGE"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Time",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "13 May 2022, 14:48",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Transaction ID",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x5f1e2506dec52d84c85b3510275b64392270c1c976dbafd8906a220e35cfc7cd",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "text": "Copy Transaction ID",
                "style": "secondaryButton",
                "actionType": "COPY",
                "actionData": "0x5f1e2506dec52d84c85b3510275b64392270c1c976dbafd8906a220e35cfc7cd",
                "type": "BUTTON"
              }
            ]
          }
        ],
        "floatingActions": [
          {
            "text": "View on Explorer",
            "style": "primary",
            "actionType": "OPEN_URL",
            "actionData": "https://polygonscan.com/tx/0x5f1e2506dec52d84c85b3510275b64392270c1c976dbafd8906a220e35cfc7cd",
            "type": "BUTTON"
          }
        ],
        "type": "GROUPED_ITEMS"
      },
      "state": "COMPLETED",
      "timestamp": 1652453294
    },
    {
      "id": "0x7b29921e56a80628c00b8b8201a59e6a6275dd1282c91717d3200560eca7d695",
      "externalUrl": "https://polygonscan.com/tx/0x7b29921e56a80628c00b8b8201a59e6a6275dd1282c91717d3200560eca7d695",
      "item": {
        "leadingImage": {
          "url": "https://login.blockchain.com/static/asset/icon/signature.svg",
          "type": "SINGLE_ICON"
        },
        "leading": [
          {
            "value": "Contract Interaction",
            "style": {
              "style": "paragraph2",
              "color": "text-title"
            },
            "type": "TEXT"
          },
          {
            "value": "13 May 2022",
            "style": {
              "style": "caption1",
              "color": "text-body"
            },
            "type": "TEXT"
          }
        ],
        "trailing": [],
        "type": "STACK_VIEW"
      },
      "detail": {
        "title": "Contract Interaction",
        "subtitle": "",
        "icon": {
          "url": "https://login.blockchain.com/static/asset/icon/signature.svg",
          "type": "SINGLE_ICON"
        },
        "itemGroups": [
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Contract Address",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x2791bca1f2de4661ed88a30c99a7a9449aa84174",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              }
            ]
          },
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Network fee",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0.004132092 MATIC",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  },
                  {
                    "value": "0.00 USD",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Status",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "Completed",
                    "style": "successBadge",
                    "type": "BADGE"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Time",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "13 May 2022, 14:25",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Transaction ID",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x7b29921e56a80628c00b8b8201a59e6a6275dd1282c91717d3200560eca7d695",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "text": "Copy Transaction ID",
                "style": "secondaryButton",
                "actionType": "COPY",
                "actionData": "0x7b29921e56a80628c00b8b8201a59e6a6275dd1282c91717d3200560eca7d695",
                "type": "BUTTON"
              }
            ]
          }
        ],
        "floatingActions": [
          {
            "text": "View on Explorer",
            "style": "primary",
            "actionType": "OPEN_URL",
            "actionData": "https://polygonscan.com/tx/0x7b29921e56a80628c00b8b8201a59e6a6275dd1282c91717d3200560eca7d695",
            "type": "BUTTON"
          }
        ],
        "type": "GROUPED_ITEMS"
      },
      "state": "COMPLETED",
      "timestamp": 1652451937
    },
    {
      "id": "0x78b64663dc3d6d2da0f7f37a9e88af1bc1dc355bd9379e9daf1f3c353fce7ca3",
      "externalUrl": "https://polygonscan.com/tx/0x78b64663dc3d6d2da0f7f37a9e88af1bc1dc355bd9379e9daf1f3c353fce7ca3",
      "item": {
        "leadingImage": {
          "main": "https://login.blockchain.com/static/asset/icon/send.svg",
          "tag": "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/polygon/info/logo.png",
          "type": "SMALL_TAG"
        },
        "leading": [
          {
            "value": "Sent MATIC",
            "style": {
              "style": "paragraph2",
              "color": "text-title"
            },
            "type": "TEXT"
          },
          {
            "value": "13 May 2022",
            "style": {
              "style": "caption1",
              "color": "text-body"
            },
            "type": "TEXT"
          }
        ],
        "trailing": [
          {
            "value": "0.01 USD",
            "style": {
              "style": "paragraph2",
              "color": "text-title"
            },
            "type": "TEXT"
          },
          {
            "value": "0.01 MATIC",
            "style": {
              "style": "caption1",
              "color": "text-body"
            },
            "type": "TEXT"
          }
        ],
        "type": "STACK_VIEW"
      },
      "detail": {
        "title": "Sent MATIC",
        "subtitle": "0.01 MATIC",
        "icon": {
          "main": "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/polygon/info/logo.png",
          "tag": "https://login.blockchain.com/static/asset/icon/send.svg",
          "type": "SMALL_TAG"
        },
        "itemGroups": [
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Amount",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0.01 MATIC",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  },
                  {
                    "value": "0.01 USD",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "From",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x6a4e9313512962a9d4ebd7348cfcf7d0ee18a2ae",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "To",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0xdd6f9fb5aa37f2eb9c5bb1cd01c7e0a29caa605e",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              }
            ]
          },
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Network fee",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0.002772 MATIC",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  },
                  {
                    "value": "0.00 USD",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Status",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "Completed",
                    "style": "successBadge",
                    "type": "BADGE"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Time",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "13 May 2022, 14:00",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Transaction ID",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x78b64663dc3d6d2da0f7f37a9e88af1bc1dc355bd9379e9daf1f3c353fce7ca3",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "text": "Copy Transaction ID",
                "style": "secondaryButton",
                "actionType": "COPY",
                "actionData": "0x78b64663dc3d6d2da0f7f37a9e88af1bc1dc355bd9379e9daf1f3c353fce7ca3",
                "type": "BUTTON"
              }
            ]
          }
        ],
        "floatingActions": [
          {
            "text": "View on Explorer",
            "style": "primary",
            "actionType": "OPEN_URL",
            "actionData": "https://polygonscan.com/tx/0x78b64663dc3d6d2da0f7f37a9e88af1bc1dc355bd9379e9daf1f3c353fce7ca3",
            "type": "BUTTON"
          }
        ],
        "type": "GROUPED_ITEMS"
      },
      "state": "COMPLETED",
      "timestamp": 1652450436
    },
    {
      "id": "0x0449baaee39cfb725fa2801d08ad94d19e173d048fb82b623139c27864f450bd",
      "externalUrl": "https://polygonscan.com/tx/0x0449baaee39cfb725fa2801d08ad94d19e173d048fb82b623139c27864f450bd",
      "item": {
        "leadingImage": {
          "url": "https://login.blockchain.com/static/asset/icon/signature.svg",
          "type": "SINGLE_ICON"
        },
        "leading": [
          {
            "value": "Contract Interaction",
            "style": {
              "style": "paragraph2",
              "color": "text-title"
            },
            "type": "TEXT"
          },
          {
            "value": "5 May 2022",
            "style": {
              "style": "caption1",
              "color": "text-body"
            },
            "type": "TEXT"
          }
        ],
        "trailing": [],
        "type": "STACK_VIEW"
      },
      "detail": {
        "title": "Contract Interaction",
        "subtitle": "",
        "icon": {
          "url": "https://login.blockchain.com/static/asset/icon/signature.svg",
          "type": "SINGLE_ICON"
        },
        "itemGroups": [
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Contract Address",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0xdc8fa3fab8421ff44cc6ca7f966673ff6c0b3b58",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              }
            ]
          },
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Network fee",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0.087475706222928898 MATIC",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  },
                  {
                    "value": "0.10 USD",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Status",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "Completed",
                    "style": "successBadge",
                    "type": "BADGE"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Time",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "5 May 2022, 13:50",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Transaction ID",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x0449baaee39cfb725fa2801d08ad94d19e173d048fb82b623139c27864f450bd",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "text": "Copy Transaction ID",
                "style": "secondaryButton",
                "actionType": "COPY",
                "actionData": "0x0449baaee39cfb725fa2801d08ad94d19e173d048fb82b623139c27864f450bd",
                "type": "BUTTON"
              }
            ]
          }
        ],
        "floatingActions": [
          {
            "text": "View on Explorer",
            "style": "primary",
            "actionType": "OPEN_URL",
            "actionData": "https://polygonscan.com/tx/0x0449baaee39cfb725fa2801d08ad94d19e173d048fb82b623139c27864f450bd",
            "type": "BUTTON"
          }
        ],
        "type": "GROUPED_ITEMS"
      },
      "state": "COMPLETED",
      "timestamp": 1651758656
    },
    {
      "id": "0x3bc66460702c940a37deb817ad6bb92e1cedb55a4df0506e4480dfe6a141d4db",
      "externalUrl": "https://polygonscan.com/tx/0x3bc66460702c940a37deb817ad6bb92e1cedb55a4df0506e4480dfe6a141d4db",
      "item": {
        "leadingImage": {
          "main": "https://login.blockchain.com/static/asset/icon/send.svg",
          "tag": "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/polygon/info/logo.png",
          "type": "SMALL_TAG"
        },
        "leading": [
          {
            "value": "Sent MATIC",
            "style": {
              "style": "paragraph2",
              "color": "text-title"
            },
            "type": "TEXT"
          },
          {
            "value": "5 May 2022",
            "style": {
              "style": "caption1",
              "color": "text-body"
            },
            "type": "TEXT"
          }
        ],
        "trailing": [
          {
            "value": "0.01 USD",
            "style": {
              "style": "paragraph2",
              "color": "text-title"
            },
            "type": "TEXT"
          },
          {
            "value": "0.01 MATIC",
            "style": {
              "style": "caption1",
              "color": "text-body"
            },
            "type": "TEXT"
          }
        ],
        "type": "STACK_VIEW"
      },
      "detail": {
        "title": "Sent MATIC",
        "subtitle": "0.01 MATIC",
        "icon": {
          "main": "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/polygon/info/logo.png",
          "tag": "https://login.blockchain.com/static/asset/icon/send.svg",
          "type": "SMALL_TAG"
        },
        "itemGroups": [
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Amount",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0.01 MATIC",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  },
                  {
                    "value": "0.01 USD",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "From",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x6a4e9313512962a9d4ebd7348cfcf7d0ee18a2ae",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "To",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0xdd6f9fb5aa37f2eb9c5bb1cd01c7e0a29caa605e",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              }
            ]
          },
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Network fee",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0.00105 MATIC",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  },
                  {
                    "value": "0.00 USD",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Status",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "Completed",
                    "style": "successBadge",
                    "type": "BADGE"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Time",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "5 May 2022, 13:49",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Transaction ID",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x3bc66460702c940a37deb817ad6bb92e1cedb55a4df0506e4480dfe6a141d4db",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "text": "Copy Transaction ID",
                "style": "secondaryButton",
                "actionType": "COPY",
                "actionData": "0x3bc66460702c940a37deb817ad6bb92e1cedb55a4df0506e4480dfe6a141d4db",
                "type": "BUTTON"
              }
            ]
          }
        ],
        "floatingActions": [
          {
            "text": "View on Explorer",
            "style": "primary",
            "actionType": "OPEN_URL",
            "actionData": "https://polygonscan.com/tx/0x3bc66460702c940a37deb817ad6bb92e1cedb55a4df0506e4480dfe6a141d4db",
            "type": "BUTTON"
          }
        ],
        "type": "GROUPED_ITEMS"
      },
      "state": "COMPLETED",
      "timestamp": 1651758558
    },
    {
      "id": "0x31ed827822852809ec3b294b3e9cf4de9ebdb06571ecdd77275da6c0a4decb8e",
      "externalUrl": "https://polygonscan.com/tx/0x31ed827822852809ec3b294b3e9cf4de9ebdb06571ecdd77275da6c0a4decb8e",
      "item": {
        "leadingImage": {
          "url": "https://login.blockchain.com/static/asset/icon/signature.svg",
          "type": "SINGLE_ICON"
        },
        "leading": [
          {
            "value": "Contract Interaction",
            "style": {
              "style": "paragraph2",
              "color": "text-title"
            },
            "type": "TEXT"
          },
          {
            "value": "28 Apr 2022",
            "style": {
              "style": "caption1",
              "color": "text-body"
            },
            "type": "TEXT"
          }
        ],
        "trailing": [],
        "type": "STACK_VIEW"
      },
      "detail": {
        "title": "Contract Interaction",
        "subtitle": "",
        "icon": {
          "url": "https://login.blockchain.com/static/asset/icon/signature.svg",
          "type": "SINGLE_ICON"
        },
        "itemGroups": [
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Contract Address",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x2791bca1f2de4661ed88a30c99a7a9449aa84174",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              }
            ]
          },
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Network fee",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0.00254112 MATIC",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  },
                  {
                    "value": "0.00 USD",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Status",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "Completed",
                    "style": "successBadge",
                    "type": "BADGE"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Time",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "28 Apr 2022, 09:20",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Transaction ID",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x31ed827822852809ec3b294b3e9cf4de9ebdb06571ecdd77275da6c0a4decb8e",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "text": "Copy Transaction ID",
                "style": "secondaryButton",
                "actionType": "COPY",
                "actionData": "0x31ed827822852809ec3b294b3e9cf4de9ebdb06571ecdd77275da6c0a4decb8e",
                "type": "BUTTON"
              }
            ]
          }
        ],
        "floatingActions": [
          {
            "text": "View on Explorer",
            "style": "primary",
            "actionType": "OPEN_URL",
            "actionData": "https://polygonscan.com/tx/0x31ed827822852809ec3b294b3e9cf4de9ebdb06571ecdd77275da6c0a4decb8e",
            "type": "BUTTON"
          }
        ],
        "type": "GROUPED_ITEMS"
      },
      "state": "COMPLETED",
      "timestamp": 1651137650
    },
    {
      "id": "0x1c215b6dd333aebc46782c5613ecb84aeee615c9aa8a35c4f7ca81694a038b93",
      "externalUrl": "https://polygonscan.com/tx/0x1c215b6dd333aebc46782c5613ecb84aeee615c9aa8a35c4f7ca81694a038b93",
      "item": {
        "leadingImage": {
          "url": "https://login.blockchain.com/static/asset/icon/signature.svg",
          "type": "SINGLE_ICON"
        },
        "leading": [
          {
            "value": "Contract Interaction",
            "style": {
              "style": "paragraph2",
              "color": "text-title"
            },
            "type": "TEXT"
          },
          {
            "value": "26 Apr 2022",
            "style": {
              "style": "caption1",
              "color": "text-body"
            },
            "type": "TEXT"
          }
        ],
        "trailing": [],
        "type": "STACK_VIEW"
      },
      "detail": {
        "title": "Contract Interaction",
        "subtitle": "",
        "icon": {
          "url": "https://login.blockchain.com/static/asset/icon/signature.svg",
          "type": "SINGLE_ICON"
        },
        "itemGroups": [
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Contract Address",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x2791bca1f2de4661ed88a30c99a7a9449aa84174",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              }
            ]
          },
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Network fee",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0.002971392 MATIC",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  },
                  {
                    "value": "0.00 USD",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Status",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "Completed",
                    "style": "successBadge",
                    "type": "BADGE"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Time",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "26 Apr 2022, 19:50",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Transaction ID",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x1c215b6dd333aebc46782c5613ecb84aeee615c9aa8a35c4f7ca81694a038b93",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "text": "Copy Transaction ID",
                "style": "secondaryButton",
                "actionType": "COPY",
                "actionData": "0x1c215b6dd333aebc46782c5613ecb84aeee615c9aa8a35c4f7ca81694a038b93",
                "type": "BUTTON"
              }
            ]
          }
        ],
        "floatingActions": [
          {
            "text": "View on Explorer",
            "style": "primary",
            "actionType": "OPEN_URL",
            "actionData": "https://polygonscan.com/tx/0x1c215b6dd333aebc46782c5613ecb84aeee615c9aa8a35c4f7ca81694a038b93",
            "type": "BUTTON"
          }
        ],
        "type": "GROUPED_ITEMS"
      },
      "state": "COMPLETED",
      "timestamp": null
    },
    {
      "id": "0x8f76c05033d8c3863b6e5536003e06b192aa0473f752c6b9dd043b289d776726",
      "externalUrl": "https://polygonscan.com/tx/0x8f76c05033d8c3863b6e5536003e06b192aa0473f752c6b9dd043b289d776726",
      "item": {
        "leadingImage": {
          "url": "https://login.blockchain.com/static/asset/icon/signature.svg",
          "type": "SINGLE_ICON"
        },
        "leading": [
          {
            "value": "Contract Interaction",
            "style": {
              "style": "paragraph2",
              "color": "text-title"
            },
            "type": "TEXT"
          },
          {
            "value": "26 Apr 2022",
            "style": {
              "style": "caption1",
              "color": "text-body"
            },
            "type": "TEXT"
          }
        ],
        "trailing": [],
        "type": "STACK_VIEW"
      },
      "detail": {
        "title": "Contract Interaction",
        "subtitle": "",
        "icon": {
          "url": "https://login.blockchain.com/static/asset/icon/signature.svg",
          "type": "SINGLE_ICON"
        },
        "itemGroups": [
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Contract Address",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x2791bca1f2de4661ed88a30c99a7a9449aa84174",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              }
            ]
          },
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Network fee",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0.003152734854819096 MATIC",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  },
                  {
                    "value": "0.00 USD",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Status",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "Completed",
                    "style": "successBadge",
                    "type": "BADGE"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Time",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "26 Apr 2022, 17:18",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Transaction ID",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x8f76c05033d8c3863b6e5536003e06b192aa0473f752c6b9dd043b289d776726",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "text": "Copy Transaction ID",
                "style": "secondaryButton",
                "actionType": "COPY",
                "actionData": "0x8f76c05033d8c3863b6e5536003e06b192aa0473f752c6b9dd043b289d776726",
                "type": "BUTTON"
              }
            ]
          }
        ],
        "floatingActions": [
          {
            "text": "View on Explorer",
            "style": "primary",
            "actionType": "OPEN_URL",
            "actionData": "https://polygonscan.com/tx/0x8f76c05033d8c3863b6e5536003e06b192aa0473f752c6b9dd043b289d776726",
            "type": "BUTTON"
          }
        ],
        "type": "GROUPED_ITEMS"
      },
      "state": "COMPLETED",
      "timestamp": 1650993524
    },
    {
      "id": "0x0c254ec358924583c779be03e406518d7ba12660d7e2485b33989e9d513d4c60",
      "externalUrl": "https://polygonscan.com/tx/0x0c254ec358924583c779be03e406518d7ba12660d7e2485b33989e9d513d4c60",
      "item": {
        "leadingImage": {
          "main": "https://login.blockchain.com/static/asset/icon/receive.svg",
          "tag": "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/polygon/info/logo.png",
          "type": "SMALL_TAG"
        },
        "leading": [
          {
            "value": "Received MATIC",
            "style": {
              "style": "paragraph2",
              "color": "text-title"
            },
            "type": "TEXT"
          },
          {
            "value": "26 Apr 2022",
            "style": {
              "style": "caption1",
              "color": "text-body"
            },
            "type": "TEXT"
          }
        ],
        "trailing": [
          {
            "value": "0.14 USD",
            "style": {
              "style": "paragraph2",
              "color": "text-title"
            },
            "type": "TEXT"
          },
          {
            "value": "0.1 MATIC",
            "style": {
              "style": "caption1",
              "color": "text-body"
            },
            "type": "TEXT"
          }
        ],
        "type": "STACK_VIEW"
      },
      "detail": {
        "title": "Received MATIC",
        "subtitle": "0.1 MATIC",
        "icon": {
          "main": "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/polygon/info/logo.png",
          "tag": "https://login.blockchain.com/static/asset/icon/receive.svg",
          "type": "SMALL_TAG"
        },
        "itemGroups": [
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Amount",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0.1 MATIC",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  },
                  {
                    "value": "0.14 USD",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "From",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0xfe928656a7c2c55cfa9bb3588738b20f3fc517a8",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "To",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x6a4e9313512962a9d4ebd7348cfcf7d0ee18a2ae",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              }
            ]
          },
          {
            "title": null,
            "itemGroup": [
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Network fee",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0.00118391688744 MATIC",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  },
                  {
                    "value": "0.00 USD",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Status",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "Completed",
                    "style": "successBadge",
                    "type": "BADGE"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Time",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "26 Apr 2022, 17:17",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "leadingImage": null,
                "leading": [
                  {
                    "value": "Transaction ID",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-body"
                    },
                    "type": "TEXT"
                  }
                ],
                "trailing": [
                  {
                    "value": "0x0c254ec358924583c779be03e406518d7ba12660d7e2485b33989e9d513d4c60",
                    "style": {
                      "style": "paragraph2",
                      "color": "text-title"
                    },
                    "type": "TEXT"
                  }
                ],
                "type": "STACK_VIEW"
              },
              {
                "text": "Copy Transaction ID",
                "style": "secondaryButton",
                "actionType": "COPY",
                "actionData": "0x0c254ec358924583c779be03e406518d7ba12660d7e2485b33989e9d513d4c60",
                "type": "BUTTON"
              }
            ]
          }
        ],
        "floatingActions": [
          {
            "text": "View on Explorer",
            "style": "primary",
            "actionType": "OPEN_URL",
            "actionData": "https://polygonscan.com/tx/0x0c254ec358924583c779be03e406518d7ba12660d7e2485b33989e9d513d4c60",
            "type": "BUTTON"
          }
        ],
        "type": "GROUPED_ITEMS"
      },
      "state": "COMPLETED",
      "timestamp": 1650993430
    }
  ]
}"""
}
