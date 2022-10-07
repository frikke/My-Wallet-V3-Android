package com.blockchain.nfts

import com.blockchain.nfts.collection.NftCollectionFragment
import com.blockchain.nfts.detail.NftDetailFragment
import com.blockchain.nfts.help.NftHelpFragment

interface NftHost : NftCollectionFragment.Host, NftDetailFragment.Host, NftHelpFragment.Host
